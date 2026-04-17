package enble.flashdeal.domain.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponIssuanceRepository extends JpaRepository<CouponIssuance, Long> {

    boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);

    long countByCouponId(Long couponId);
}
