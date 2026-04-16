package enble.flashdeal.domain.order;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.order.dto.OrderResponse;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
import enble.flashdeal.domain.product.StockService;
import enble.flashdeal.global.exception.MemberNotFoundException;
import enble.flashdeal.global.exception.OutOfStockException;
import enble.flashdeal.global.exception.ProductNotFoundException;
import enble.flashdeal.global.exception.SaleNotStartedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final StockService stockService;

    /**
     * Phase 3 — Redis DECR 원자 연산으로 재고 차감.
     *
     * DB SELECT FOR UPDATE를 제거하고 Redis DECR로 재고 게이트를 대체한다.
     * DECR은 Redis 단일 스레드 명령 실행 모델에 의해 원자적으로 처리되므로
     * 락 없이도 오버셀이 방지된다.
     * DB 커넥션은 주문 INSERT에만 사용되므로 HikariCP 병목이 해소된다.
     */
    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isSaleStarted()) {
            throw new SaleNotStartedException();
        }

        if (!stockService.decrease(request.productId(), request.quantity())) {
            throw new OutOfStockException();
        }

        Order order = Order.create(member, product, request.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }
}
