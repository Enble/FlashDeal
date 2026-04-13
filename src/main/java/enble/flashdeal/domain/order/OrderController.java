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
}
