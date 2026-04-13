package enble.flashdeal.domain.order.dto;

import enble.flashdeal.domain.order.Order;
import enble.flashdeal.domain.order.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long memberId,
        Long productId,
        int quantity,
        OrderStatus status,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getMember().getId(),
                order.getProduct().getId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}
