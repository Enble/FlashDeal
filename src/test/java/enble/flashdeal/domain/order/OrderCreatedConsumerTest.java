package enble.flashdeal.domain.order;

import enble.flashdeal.domain.notification.NotificationService;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import enble.flashdeal.domain.settlement.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @InjectMocks
    private OrderCreatedConsumer consumer;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SettlementService settlementService;

    @Test
    @DisplayName("이벤트 수신 시 알림 발송과 정산이 순서대로 호출된다.")
    void consume_callsNotifyAndRecord() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());

        consumer.consume(event);

        verify(notificationService).notify(event);
        verify(settlementService).record(event);
    }

    @Test
    @DisplayName("알림 발송 실패 시 예외가 전파되어 Kafka가 재시도할 수 있도록 한다.")
    void consume_notifyFails_propagatesException() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());
        doThrow(new RuntimeException("알림 서비스 장애")).when(notificationService).notify(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> consumer.consume(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("알림 서비스 장애");
    }
}
