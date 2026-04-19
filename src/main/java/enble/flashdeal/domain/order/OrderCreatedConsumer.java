package enble.flashdeal.domain.order;

import enble.flashdeal.domain.notification.NotificationService;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import enble.flashdeal.domain.settlement.SettlementService;
import enble.flashdeal.global.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final NotificationService notificationService;
    private final SettlementService settlementService;

    /**
     * order-created 이벤트를 소비해 알림 발송과 정산을 처리한다.
     * <p>
     * 전달 보장: at-least-once (Consumer는 처리 완료 후 오프셋을 커밋한다).
     * 재시도: 최대 3회 시도(초기 포함), 지수 백오프(1s → 2s).
     * 재시도 소진 시: order-created-dlt 토픽으로 격리 — 주문 흐름에 영향 없음.
     * <p>
     * SettlementService는 (order_id, consumer_group) UNIQUE 제약으로 중복 처리를 방지한다.
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dlt",
            autoCreateTopics = "false"
    )
    @KafkaListener(topics = KafkaTopicConfig.ORDER_CREATED, groupId = "order-processor")
    public void consume(OrderCreatedEvent event) {
        log.info("[Consumer] 이벤트 수신 — orderId={}, productId={}", event.orderId(), event.productId());
        notificationService.notify(event);
        settlementService.record(event);
    }
}
