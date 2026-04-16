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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final StockService stockService;

    /**
     * Phase 3 — Redis Lua script + DB 롤백 보상으로 재고-주문 정합성 보장.
     *
     * 1. Redis Lua script로 DECR + 보상 INCR을 원자 블록으로 실행한다.
     * 2. DECR 성공 후 DB INSERT가 실패하면 TransactionSynchronization 콜백이
     *    트랜잭션 롤백 시점에 Redis INCR을 호출해 재고를 복구한다.
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

        // DB 트랜잭션이 롤백되면 차감된 Redis 재고를 복구한다.
        // isSynchronizationActive() 가드: 트랜잭션 없이 호출되는 단위 테스트 환경에서 예외 방지
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        stockService.increase(request.productId(), request.quantity());
                    }
                }
            });
        }

        Order order = Order.create(member, product, request.quantity());
        return OrderResponse.from(orderRepository.save(order));
    }
}
