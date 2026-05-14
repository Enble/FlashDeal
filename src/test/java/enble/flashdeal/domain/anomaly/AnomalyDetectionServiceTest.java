package enble.flashdeal.domain.anomaly;

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
    @Mock AiReportGenerator aiReportGenerator;

    @InjectMocks AnomalyDetectionService anomalyDetectionService;

    private final OrderCreatedEvent event = new OrderCreatedEvent(10L, 1L, 42L, 1, LocalDateTime.now());

    @Test
    @DisplayName("최근 10분 내 주문이 1건이면 이상 탐지를 건너뛴다.")
    void detect_단건주문_저장안함() {
        given(orderRepository.findRecentOrdersByMemberId(eq(42L), any()))
                .willReturn(List.of(mock(Order.class)));

        anomalyDetectionService.detect(event);

        then(anomalyReportRepository).shouldHaveNoInteractions();
        then(aiReportGenerator).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("최근 10분 내 2건 이상 주문 시 AI 리포트를 생성하고 저장한다.")
    void detect_반복주문_리포트저장() {
        given(orderRepository.findRecentOrdersByMemberId(eq(42L), any()))
                .willReturn(List.of(mock(Order.class), mock(Order.class)));
        given(aiReportGenerator.generate(eq(42L), any())).willReturn("AI 요약 내용");

        anomalyDetectionService.detect(event);

        ArgumentCaptor<AnomalyReport> captor = ArgumentCaptor.forClass(AnomalyReport.class);
        then(anomalyReportRepository).should().save(captor.capture());
        AnomalyReport saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(42L);
        assertThat(saved.getTriggerOrderId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(AnomalyStatus.AI_COMPLETED);
        assertThat(saved.getAiSummary()).isEqualTo("AI 요약 내용");
    }

    @Test
    @DisplayName("AI 호출 실패 시 status=AI_FAILED로 리포트를 저장한다.")
    void detect_AI호출실패_AI_FAILED저장() {
        given(orderRepository.findRecentOrdersByMemberId(eq(42L), any()))
                .willReturn(List.of(mock(Order.class), mock(Order.class)));
        given(aiReportGenerator.generate(any(), any())).willThrow(new RuntimeException("API error"));

        anomalyDetectionService.detect(event);

        ArgumentCaptor<AnomalyReport> captor = ArgumentCaptor.forClass(AnomalyReport.class);
        then(anomalyReportRepository).should().save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AnomalyStatus.AI_FAILED);
        assertThat(captor.getValue().getAiSummary()).isNull();
    }
}
