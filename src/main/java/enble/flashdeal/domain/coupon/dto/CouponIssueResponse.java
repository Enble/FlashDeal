package enble.flashdeal.domain.coupon.dto;

import enble.flashdeal.domain.coupon.CouponIssuance;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long id,
        Long couponId,
        Long memberId,
        LocalDateTime issuedAt
) {
    public static CouponIssueResponse from(CouponIssuance issuance) {
        return new CouponIssueResponse(
                issuance.getId(),
                issuance.getCoupon().getId(),
                issuance.getMember().getId(),
                issuance.getIssuedAt()
        );
    }
}
