package enble.flashdeal.domain.settlement;

import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    static final String CONSUMER_GROUP = "order-processor";
    private static final long SIMULATED_DELAY_MS = 100;

    private final ProcessedOrderEventRepository processedOrderEventRepository;

    /**
     * 정산 처리 — at-least-once 환경에서 중복 실행을 방지하는 멱등성 구현.
     * <p>
     * Kafka 기본 보장은 at-least-once이므로 Consumer 재시작 시 같은 이벤트가 다시 전달될 수 있다.
     * processed_order_events 테이블에 (order_id, consumer_group) UNIQUE 제약을 두어
     * 이미 처리된 이벤트를 스킵한다.
     */
    @Transactional
    public void record(OrderCreatedEvent event) {
        if (processedOrderEventRepository.existsByOrderIdAndConsumerGroup(event.orderId(), CONSUMER_GROUP)) {
            log.warn("[정산] 중복 이벤트 스킵 — orderId={}", event.orderId());
            return;
        }
        processedOrderEventRepository.save(ProcessedOrderEvent.of(event.orderId(), CONSUMER_GROUP));

        try {
            Thread.sleep(SIMULATED_DELAY_MS);
            log.info("[정산] 완료 — orderId={}, productId={}, quantity={}",
                    event.orderId(), event.productId(), event.quantity());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
