package enble.flashdeal.domain.notification;

import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatNoException;

class NotificationServiceTest {

    private final NotificationService notificationService = new NotificationService();

    @Test
    @DisplayName("알림 발송 시 예외 없이 완료된다.")
    void notify_completesWithoutException() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());

        assertThatNoException().isThrownBy(() -> notificationService.notify(event));
    }

    @Test
    @DisplayName("알림 발송에는 시뮬레이션 지연(150ms)이 포함된다 — Phase 5 동기 후처리 응답시간 근거.")
    void notify_includesSimulatedDelay() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 1, LocalDateTime.now());
        long start = System.nanoTime();

        notificationService.notify(event);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        org.assertj.core.api.Assertions.assertThat(elapsedMs).isGreaterThanOrEqualTo(150);
    }
}
