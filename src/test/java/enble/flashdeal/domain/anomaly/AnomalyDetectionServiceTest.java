package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.anomaly.event.AnomalyDetectedEvent;
import enble.flashdeal.domain.order.Order;
import enble.flashdeal.domain.order.OrderRepository;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock AnomalyReportRepository anomalyReportRepository;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks AnomalyDetectionService anomalyDetectionService;

    private final OrderCreatedEvent event = new OrderCreatedEvent(10L, 1L, 42L, 1, LocalDateTime.now());

    @Test
    @DisplayName("최근 10분 내 주문이 1건이면 이상 탐지를 건너뛴다.")
    void detect_단건주문_저장안함() {
        given(orderRepository.findRecentOrdersByMemberId(eq(42L), any()))
                .willReturn(List.of(mock(Order.class)));

        anomalyDetectionService.detect(event);

        then(anomalyReportRepository).shouldHaveNoInteractions();
        then(kafkaTemplate).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최근 10분 내 2건 이상 주문 시 PENDING 상태로 저장하고 Kafka 이벤트를 발행한다.")
    void detect_반복주문_PENDING저장후_Kafka발행() {
        Order order1 = mock(Order.class);
        Order order2 = mock(Order.class);
        given(order1.getId()).willReturn(10L);
        given(order1.getProduct()).willReturn(mock(enble.flashdeal.domain.product.Product.class));
        given(order2.getId()).willReturn(11L);
        given(order2.getProduct()).willReturn(mock(enble.flashdeal.domain.product.Product.class));
        given(orderRepository.findRecentOrdersByMemberId(eq(42L), any()))
                .willReturn(List.of(order1, order2));

        AnomalyReport savedReport = AnomalyReport.createPending(42L, 10L, "reason");
        given(anomalyReportRepository.save(any())).willReturn(savedReport);

        anomalyDetectionService.detect(event);

        ArgumentCaptor<AnomalyReport> reportCaptor = ArgumentCaptor.forClass(AnomalyReport.class);
        then(anomalyReportRepository).should().save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(AnomalyStatus.PENDING);
        assertThat(reportCaptor.getValue().getAiSummary()).isNull();

        then(kafkaTemplate).should().send(eq("anomaly-detected"), any(AnomalyDetectedEvent.class));
    }
}
