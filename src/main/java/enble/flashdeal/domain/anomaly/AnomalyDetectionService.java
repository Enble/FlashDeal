package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.anomaly.event.AnomalyDetectedEvent;
import enble.flashdeal.domain.anomaly.event.AnomalyDetectedEvent.OrderSummary;
import enble.flashdeal.domain.order.Order;
import enble.flashdeal.domain.order.OrderRepository;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import enble.flashdeal.global.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    static final int DETECTION_WINDOW_MINUTES = 10;
    static final int ANOMALY_THRESHOLD = 2;

    private final OrderRepository orderRepository;
    private final AnomalyReportRepository anomalyReportRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void detect(OrderCreatedEvent event) {
        LocalDateTime windowStart = event.occurredAt().minusMinutes(DETECTION_WINDOW_MINUTES);
        List<Order> recentOrders = orderRepository.findRecentOrdersByMemberId(event.memberId(), windowStart);

        if (recentOrders.size() < ANOMALY_THRESHOLD) {
            return;
        }

        String reason = String.format("최근 %d분 내 %d건 주문 감지 (임계값: %d건)",
                DETECTION_WINDOW_MINUTES, recentOrders.size(), ANOMALY_THRESHOLD);

        log.warn("[이상 탐지] 비정상 패턴 감지 — memberId={}, {}", event.memberId(), reason);

        AnomalyReport report = anomalyReportRepository.save(
                AnomalyReport.createPending(event.memberId(), event.orderId(), reason));

        List<OrderSummary> summaries = recentOrders.stream()
                .map(o -> new OrderSummary(o.getId(), o.getProduct().getId(), o.getQuantity(),
                        o.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .toList();

        kafkaTemplate.send(KafkaTopicConfig.ANOMALY_DETECTED,
                new AnomalyDetectedEvent(report.getId(), event.memberId(), event.orderId(), reason, summaries));

        log.info("[이상 탐지] anomaly-detected 이벤트 발행 — reportId={}", report.getId());
    }
}
