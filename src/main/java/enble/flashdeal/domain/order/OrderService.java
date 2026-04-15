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
     * Phase 2 — SELECT FOR UPDATE(비관적 락)으로 동시성 제어.
     *
     * 상품 조회 시점에 배타 락을 획득해 read-modify-write를 직렬화한다.
     * 데드락과 lost update가 해소되는 대신, 트랜잭션이 락을 보유하는 동안
     * 다른 트랜잭션이 블로킹되어 고동시 환경에서 TPS가 감소한다.
     */
    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);

        Product product = productRepository.findByIdWithLock(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isSaleStarted()) {
            throw new SaleNotStartedException();
        }

        if (product.getStockQuantity() < request.quantity()) {
            throw new OutOfStockException();
        }

        product.decreaseStock(request.quantity());

        Order order = Order.create(member, product, request.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }
}
