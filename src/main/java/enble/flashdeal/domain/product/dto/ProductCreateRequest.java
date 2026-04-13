package enble.flashdeal.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ProductCreateRequest(
        @NotBlank(message = "상품명을 입력해주세요.")
        String name,

        @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        int price,

        @Min(value = 1, message = "재고는 1개 이상이어야 합니다.")
        int stockQuantity,

        @NotNull(message = "판매 시작 시간을 입력해주세요.")
        LocalDateTime saleStartAt
) {
}
