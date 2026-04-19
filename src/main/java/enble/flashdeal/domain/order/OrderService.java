package enble.flashdeal.domain.order;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.notification.NotificationService;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.order.dto.OrderResponse;
import enble.flashdeal.domain.order.event.OrderCreatedEvent;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
import enble.flashdeal.domain.product.StockService;
import enble.flashdeal.domain.settlement.SettlementService;
import enble.flashdeal.global.exception.MemberNotFoundException;
import enble.flashdeal.global.exception.OutOfStockException;
import enble.flashdeal.global.exception.ProductNotFoundException;
import enble.flashdeal.global.exception.SaleNotStartedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final SettlementService settlementService;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Phase 3 — Redis Lua script + DB 롤백 보상으로 재고-주문 정합성 보장.
     */
    @Transactional
    public OrderResponse placeOrder(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isSaleStarted()) throw new SaleNotStartedException();
        if (!stockService.decrease(request.productId(), request.quantity())) throw new OutOfStockException();

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

    /**
     * Phase 5 — 문제 재현: 주문 후 알림(150ms) + 정산(100ms)을 동기로 호출.
     * <p>
     * 두 가지 문제를 재현한다.
     * 1. 응답시간 증가: 후처리 합산 시간(~250ms)이 P99에 직접 반영된다.
     * 2. 장애 전파: notify()가 예외를 던지면 @Transactional이 주문 전체를 롤백한다.
     */
    @Transactional
    public OrderResponse placeOrderWithSyncPost(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isSaleStarted()) throw new SaleNotStartedException();
        if (!stockService.decrease(request.productId(), request.quantity())) throw new OutOfStockException();

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

        Order order = orderRepository.save(Order.create(member, product, request.quantity()));
        OrderCreatedEvent event = toEvent(order, request);

        notificationService.notify(event);
        settlementService.record(event);

        return OrderResponse.from(order);
    }

    /**
     * Phase 5 — 해결: 주문 저장 후 order-created 이벤트를 Kafka에 발행하고 즉시 응답.
     * <p>
     * 트랜잭션 커밋 완료 후 이벤트를 발행한다.
     * 롤백 시 이벤트가 발행되지 않아 Consumer가 존재하지 않는 주문을 처리하는 상황을 방지한다.
     */
    @Transactional
    public OrderResponse placeOrderWithAsyncPost(OrderCreateRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(MemberNotFoundException::new);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isSaleStarted()) throw new SaleNotStartedException();
        if (!stockService.decrease(request.productId(), request.quantity())) throw new OutOfStockException();

        Order order = orderRepository.save(Order.create(member, product, request.quantity()));
        OrderCreatedEvent event = toEvent(order, request);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    orderEventPublisher.publish(event);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        stockService.increase(request.productId(), request.quantity());
                    }
                }
            });
        }

        return OrderResponse.from(order);
    }

    private OrderCreatedEvent toEvent(Order order, OrderCreateRequest request) {
        return new OrderCreatedEvent(
                order.getId(),
                request.productId(),
                request.memberId(),
                request.quantity(),
                LocalDateTime.now()
        );
    }
}
