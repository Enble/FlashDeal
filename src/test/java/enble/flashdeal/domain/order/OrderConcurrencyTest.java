package enble.flashdeal.domain.order;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
import enble.flashdeal.domain.product.StockService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrderConcurrencyTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private StockService stockService;

    private static final int INITIAL_STOCK = 100;

    private Member testMember;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        testMember = memberRepository.save(Member.create("테스트유저", "test@test.com"));
        testProduct = productRepository.save(
                Product.create("테스트상품", 10_000, INITIAL_STOCK, LocalDateTime.now().minusHours(1))
        );
        stockService.initStock(testProduct.getId(), INITIAL_STOCK);
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Redis DECR로 200개 동시 요청을 처리하면 주문 수와 Redis 차감량이 일치한다.")
    void Redis_DECR_적용_시_데이터_정합성_보장() throws InterruptedException {
        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    orderService.placeOrder(new OrderCreateRequest(testMember.getId(), testProduct.getId(), 1));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long orderCount = orderRepository.count();
        long remainingStock = stockService.getStock(testProduct.getId());
        long stockConsumed = INITIAL_STOCK - remainingStock;

        System.out.println("=== Phase 3 Redis DECR 결과 ===");
        System.out.println("초기 재고        : " + INITIAL_STOCK);
        System.out.println("총 요청 수       : " + threadCount);
        System.out.println("성공 주문 수(앱)  : " + successCount.get());
        System.out.println("실패 주문 수(앱)  : " + failCount.get());
        System.out.println("Redis 잔여 재고  : " + remainingStock);
        System.out.println("Redis 차감량     : " + stockConsumed);
        System.out.println("DB 주문 건수     : " + orderCount);
        System.out.println("--- 데이터 정합성 ---");
        System.out.println("주문 건수 == Redis 차감량 : " + orderCount + " == " + stockConsumed
                + " → " + (orderCount == stockConsumed ? "일치 (정합성 보장)" : "불일치"));

        // Redis DECR의 원자성으로 read-modify-write가 경합 없이 처리됨.
        // 재고(100개)를 초과하는 요청은 DECR 후 음수가 되면 즉시 INCR로 복구 후 OutOfStockException.
        // 결과: 주문 건수 = Redis 차감량 = 초기 재고 (오버셀 없음).
        assertThat(orderCount).isEqualTo(stockConsumed);
        assertThat(successCount.get()).isEqualTo(INITIAL_STOCK);
        assertThat(remainingStock).isZero();
    }
}
