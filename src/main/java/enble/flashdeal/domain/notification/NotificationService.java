package enble.flashdeal.domain.notification;

import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    private static final long SIMULATED_DELAY_MS = 150;

    public void notify(OrderCreatedEvent event) {
        try {
            Thread.sleep(SIMULATED_DELAY_MS);
            log.info("[알림] 발송 완료 — orderId={}, memberId={}", event.orderId(), event.memberId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
