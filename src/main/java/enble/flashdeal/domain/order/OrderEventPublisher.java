package enble.flashdeal.domain.order;

import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import enble.flashdeal.global.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    /**
     * order-created 토픽에 이벤트를 발행한다.
     * <p>
     * productId를 파티션 키로 사용해 같은 상품의 주문 이벤트가 항상 동일 파티션에 쌓이도록 한다.
     * 파티션 내 순서 보장으로 SettlementConsumer의 재고 DB 반영 순서가 보장된다.
     */
    public void publish(OrderCreatedEvent event) {
        String partitionKey = event.productId().toString();
        kafkaTemplate.send(KafkaTopicConfig.ORDER_CREATED, partitionKey, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("[Kafka] 이벤트 발행 완료 — orderId={}, partition={}",
                                event.orderId(),
                                result.getRecordMetadata().partition());
                    } else {
                        log.error("[Kafka] 이벤트 발행 실패 — orderId={}", event.orderId(), ex);
                    }
                });
    }
}
