package enble.flashdeal.domain.order;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.order.dto.OrderResponse;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
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

    /**
     * Phase 1 — 의도적으로 동시성 제어 없이 구현.
     *
     * 재고 확인(check)과 차감(update) 사이에 다른 트랜잭션이 끼어들 수 있어
     * 동시 요청 시 재고가 음수가 되는 오버셀 현상이 발생한다.
     * JMeter로 이 현상을 수치로 확인한 뒤 Phase 2에서 Redis로 해결한다.
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

        // 재고 확인 — 여기서 통과해도 다른 트랜잭션이 동시에 통과할 수 있음
        if (product.getStockQuantity() < request.quantity()) {
            throw new OutOfStockException();
        }

        // 재고 차감 — SELECT + UPDATE 사이의 gap이 오버셀의 원인
        product.decreaseStock(request.quantity());

        Order order = Order.create(member, product, request.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }
}
