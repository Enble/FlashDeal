package enble.flashdeal.domain.order.event;

import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long productId,
        Long memberId,
        int quantity,
        LocalDateTime occurredAt
) {}
