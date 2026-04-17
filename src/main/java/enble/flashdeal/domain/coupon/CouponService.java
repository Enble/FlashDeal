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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssuanceRepository couponIssuanceRepository;
    private final MemberRepository memberRepository;

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
}
