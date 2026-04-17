package enble.flashdeal.domain.coupon.dto;

import jakarta.validation.constraints.NotNull;

public record CouponIssueRequest(
        @NotNull Long memberId
) {
}
