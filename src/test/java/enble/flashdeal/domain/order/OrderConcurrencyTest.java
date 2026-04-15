package enble.flashdeal.domain.order;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
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

    private Member testMember;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();

        testMember = memberRepository.save(Member.create("테스트유저", "test@test.com"));
        testProduct = productRepository.save(
                Product.create("테스트상품", 10_000, 100, LocalDateTime.now().minusHours(1))
        );
    }

    @AfterEach
    void tearDown() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("동시성 제어 없이 200개 요청을 동시에 처리하면 주문 수와 재고 차감량이 불일치한다.")
    void 동시_주문_시_데이터_정합성_오류_발생() throws InterruptedException {
        int initialStock = 100;
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

        Product result = productRepository.findById(testProduct.getId()).orElseThrow();
        long orderCount = orderRepository.count();
        int stockConsumed = initialStock - result.getStockQuantity();

        System.out.println("=== Phase 1 동시성 오류 재현 결과 ===");
        System.out.println("초기 재고        : " + initialStock);
        System.out.println("총 요청 수       : " + threadCount);
        System.out.println("성공 주문 수(앱)  : " + successCount.get());
        System.out.println("실패 주문 수(앱)  : " + failCount.get());
        System.out.println("최종 재고        : " + result.getStockQuantity());
        System.out.println("실제 차감된 재고  : " + stockConsumed);
        System.out.println("DB 주문 건수     : " + orderCount);
        System.out.println("--- 데이터 정합성 ---");
        System.out.println("주문 건수 > 차감량 : " + orderCount + " > " + stockConsumed
                + " → " + (orderCount > stockConsumed ? "불일치 (race condition 발생)" : "일치"));

        // 핵심: @Transactional + READ COMMITTED에서 SELECT → check → UPDATE 사이의 gap으로
        // 여러 트랜잭션이 같은 재고를 읽고 동일한 값으로 덮어씀 (lost update).
        // 결과적으로 '주문은 생성됐으나 재고는 차감되지 않은' 데이터 불일치 발생.
        assertThat(orderCount).isGreaterThan(stockConsumed);
    }

    @Test
    @DisplayName("SELECT FOR UPDATE(비관적 락)으로 200개 동시 요청을 처리하면 주문 수와 재고 차감량이 일치한다.")
    void 비관적_락_적용_시_데이터_정합성_보장() throws InterruptedException {
        int initialStock = 100;
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

        Product result = productRepository.findById(testProduct.getId()).orElseThrow();
        long orderCount = orderRepository.count();
        int stockConsumed = initialStock - result.getStockQuantity();

        System.out.println("=== Phase 2 비관적 락 결과 ===");
        System.out.println("초기 재고        : " + initialStock);
        System.out.println("총 요청 수       : " + threadCount);
        System.out.println("성공 주문 수(앱)  : " + successCount.get());
        System.out.println("실패 주문 수(앱)  : " + failCount.get());
        System.out.println("최종 재고        : " + result.getStockQuantity());
        System.out.println("실제 차감된 재고  : " + stockConsumed);
        System.out.println("DB 주문 건수     : " + orderCount);
        System.out.println("--- 데이터 정합성 ---");
        System.out.println("주문 건수 == 차감량 : " + orderCount + " == " + stockConsumed
                + " → " + (orderCount == stockConsumed ? "일치 (정합성 보장)" : "불일치"));

        // SELECT FOR UPDATE로 read-modify-write를 직렬화.
        // 재고가 소진되면 이후 요청은 OutOfStockException으로 거부되고 주문이 생성되지 않는다.
        // 결과: 주문 건수 = 실제 차감된 재고 (lost update 없음).
        assertThat(orderCount).isEqualTo(stockConsumed);
        assertThat(successCount.get()).isEqualTo((int) orderCount);
        assertThat(result.getStockQuantity()).isGreaterThanOrEqualTo(0);
    }
}
