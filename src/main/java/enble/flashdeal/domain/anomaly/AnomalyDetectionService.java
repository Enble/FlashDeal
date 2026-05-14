package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.order.Order;
import enble.flashdeal.domain.order.OrderRepository;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    static final int DETECTION_WINDOW_MINUTES = 10;
    static final int ANOMALY_THRESHOLD = 2;

    private final OrderRepository orderRepository;
    private final AnomalyReportRepository anomalyReportRepository;
    private final AiReportGenerator aiReportGenerator;

    public void detect(OrderCreatedEvent event) {
        LocalDateTime windowStart = event.occurredAt().minusMinutes(DETECTION_WINDOW_MINUTES);
        List<Order> recentOrders = orderRepository.findRecentOrdersByMemberId(event.memberId(), windowStart);

        if (recentOrders.size() < ANOMALY_THRESHOLD) {
            return;
        }

        String reason = String.format("최근 %d분 내 %d건 주문 감지 (임계값: %d건)",
                DETECTION_WINDOW_MINUTES, recentOrders.size(), ANOMALY_THRESHOLD);

        log.warn("[이상 탐지] 비정상 패턴 감지 — memberId={}, {}", event.memberId(), reason);

        String aiSummary = null;
        AnomalyStatus status;

        try {
            aiSummary = aiReportGenerator.generate(event.memberId(), recentOrders);
            status = AnomalyStatus.AI_COMPLETED;
        } catch (Exception e) {
            log.error("[이상 탐지] AI 리포트 생성 실패 — memberId={}", event.memberId(), e);
            status = AnomalyStatus.AI_FAILED;
        }

        anomalyReportRepository.save(
                AnomalyReport.create(event.memberId(), event.orderId(), reason, aiSummary, status));
    }
}
