package enble.flashdeal.domain.settlement;

import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @Test
    @DisplayName("처음 수신된 이벤트는 정상 처리되어 저장된다.")
    void record_firstTime_savesEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());
        given(processedOrderEventRepository.existsByOrderIdAndConsumerGroup(1L, SettlementService.CONSUMER_GROUP))
                .willReturn(false);

        settlementService.record(event);

        verify(processedOrderEventRepository).save(any(ProcessedOrderEvent.class));
    }

    @Test
    @DisplayName("이미 처리된 이벤트(중복 메시지)는 저장을 스킵한다 — at-least-once 멱등성 보장.")
    void record_duplicate_skipsWithoutSaving() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());
        given(processedOrderEventRepository.existsByOrderIdAndConsumerGroup(1L, SettlementService.CONSUMER_GROUP))
                .willReturn(true);

        settlementService.record(event);

        verify(processedOrderEventRepository, never()).save(any());
    }
}
