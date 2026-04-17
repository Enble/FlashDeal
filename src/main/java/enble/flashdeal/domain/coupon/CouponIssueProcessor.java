package enble.flashdeal.domain.coupon;

import enble.flashdeal.domain.coupon.dto.CouponIssueResponse;
import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.global.exception.AlreadyIssuedException;
import enble.flashdeal.global.exception.CouponExhaustedException;
import enble.flashdeal.global.exception.CouponNotFoundException;
import enble.flashdeal.global.exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 쿠폰 발급 임계 구역의 트랜잭션 처리를 담당한다.
 *
 * issueWithLock()에서 Redisson 락을 획득한 뒤 이 메서드를 호출한다.
 * 별도 빈으로 분리한 이유: Spring @Transactional은 AOP 프록시 기반이므로
 * 같은 클래스 내 호출(self-invocation)에서는 트랜잭션이 적용되지 않는다.
 *
 * 실행 순서: 락 획득 → 트랜잭션 시작 → 임계 구역 실행 → 트랜잭션 커밋 → 락 해제
 * 이 순서를 지켜야 락 해제 전에 커밋이 완료되어 다른 스레드가 최신 상태를 읽는다.
 */
@Component
@RequiredArgsConstructor
public class CouponIssueProcessor {

    private final CouponRepository couponRepository;
    private final CouponIssuanceRepository couponIssuanceRepository;
    private final MemberRepository memberRepository;

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
}
