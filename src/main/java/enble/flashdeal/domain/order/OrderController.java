package enble.flashdeal.domain.order;

import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.placeOrder(request);
    }

    /**
     * Phase 5 — 문제 재현: 알림 + 정산을 동기로 처리.
     * 후처리 지연이 응답시간에 직접 반영되고, 후처리 장애가 주문 전체를 실패시킨다.
     */
    @PostMapping("/sync-post")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrderWithSyncPost(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.placeOrderWithSyncPost(request);
    }

    /**
     * Phase 5 — 해결: 주문 완료 후 Kafka 이벤트 발행, Consumer가 후처리를 비동기로 처리.
     * 응답시간이 후처리 지연에서 분리되고, Consumer 장애는 DLT로 격리된다.
     */
    @PostMapping("/async-post")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrderWithAsyncPost(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.placeOrderWithAsyncPost(request);
    }
}
