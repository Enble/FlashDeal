package enble.flashdeal.domain.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CouponCreateRequest(
        @NotBlank String name,
        @Min(1) int totalQuantity
) {
}
