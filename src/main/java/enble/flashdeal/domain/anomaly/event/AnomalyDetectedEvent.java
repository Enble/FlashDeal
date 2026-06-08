package enble.flashdeal.domain.anomaly.event;

import java.time.LocalDateTime;
import java.util.List;

public record AnomalyDetectedEvent(
        Long reportId,
        Long memberId,
        Long triggerOrderId,
        String detectedReason,
        List<OrderSummary> recentOrders
) {
    public record OrderSummary(
            Long orderId,
            Long productId,
            Integer quantity,
            LocalDateTime createdAt
    ) {}
}
