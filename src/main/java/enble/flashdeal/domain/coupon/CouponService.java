package enble.flashdeal.domain.coupon;

import enble.flashdeal.domain.coupon.dto.CouponCreateRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueResponse;
import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.global.exception.AlreadyIssuedException;
import enble.flashdeal.global.exception.CouponExhaustedException;
import enble.flashdeal.global.exception.CouponNotFoundException;
import enble.flashdeal.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponService {

    private static final String COUPON_LOCK_PREFIX = "lock:coupon:";
    private static final long WAIT_TIME = 3L;
    private static final long LEASE_TIME = 3L;

    private final CouponRepository couponRepository;
    private final CouponIssuanceRepository couponIssuanceRepository;
    private final MemberRepository memberRepository;
    private final RedissonClient redissonClient;
    private final CouponIssueProcessor couponIssueProcessor;

    @Transactional
    public Long create(CouponCreateRequest request) {
        Coupon coupon = Coupon.create(request.name(), request.totalQuantity());
        return couponRepository.save(coupon).getId();
    }

    /**
     * 락 없는 쿠폰 발급 — 동시성 제어 미적용 (Phase 4 문제 재현용).
     *
     * "중복 확인 → 수량 확인 → 발급" 세 단계가 하나의 임계 구역으로 보호되지 않으므로
     * 고동시 환경에서 두 스레드가 동시에 수량 확인을 통과해 초과 발급이 발생한다.
     */
    @Transactional
    public CouponIssueResponse issue(Long couponId, Long memberId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        if (couponIssuanceRepository.existsByCouponIdAndMemberId(couponId, memberId)) {
            throw new AlreadyIssuedException();
        }

        long issuedCount = couponIssuanceRepository.countByCouponId(couponId);
        if (issuedCount >= coupon.getTotalQuantity()) {
            throw new CouponExhaustedException();
        }

        CouponIssuance issuance = couponIssuanceRepository.save(CouponIssuance.create(coupon, member));
        return CouponIssueResponse.from(issuance);
    }

    /**
     * Redisson 분산 락을 적용한 쿠폰 발급.
     *
     * 쿠폰 단위(lock:coupon:{couponId})로 락을 걸어
     * "중복 확인 → 수량 확인 → 발급" 세 단계를 직렬화한다.
     *
     * 실행 순서: 락 획득 → 트랜잭션 시작 → 임계 구역 실행 → 트랜잭션 커밋 → 락 해제.
     * @Transactional을 이 메서드에 붙이지 않는 이유: 트랜잭션이 락보다 먼저 시작되면
     * 커밋 전에 락이 해제돼 다른 스레드가 커밋 전 상태를 읽을 수 있기 때문이다.
     * 트랜잭션은 CouponIssueProcessor에 위임해 락 범위 안에서만 열리도록 한다.
     */
    public CouponIssueResponse issueWithLock(Long couponId, Long memberId) {
        RLock lock = redissonClient.getLock(COUPON_LOCK_PREFIX + couponId);
        try {
            if (!lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.SECONDS)) {
                throw new CouponExhaustedException();
            }
            return couponIssueProcessor.issue(couponId, memberId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CouponExhaustedException();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
