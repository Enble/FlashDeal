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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Redis DECR 성공 후 DB INSERT 실패 시 Redis 재고가 복구되는지 검증한다.
 *
 * TransactionSynchronizationManager 콜백이 롤백 시 stockService.increase()를 호출하는지 확인.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderRollbackCompensationTest {

    @Autowired private OrderService orderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StockService stockService;

    @MockitoBean
    private OrderRepository orderRepository;  // DB INSERT 실패 시뮬레이션용

    private Member testMember;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        memberRepository.deleteAll();

        testMember = memberRepository.save(Member.create("테스트유저", "test@test.com"));
        testProduct = productRepository.save(
                Product.create("테스트상품", 10_000, 100, LocalDateTime.now().minusHours(1))
        );
        stockService.initStock(testProduct.getId(), 100);
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("DB INSERT 실패(롤백) 시 Redis DECR 차감량이 INCR로 복구된다.")
    void dbFailure_restoresRedisStock() {
        given(orderRepository.save(any())).willThrow(new RuntimeException("DB 장애 시뮬레이션"));

        assertThatThrownBy(() -> orderService.placeOrder(
                new OrderCreateRequest(testMember.getId(), testProduct.getId(), 1)))
                .isInstanceOf(RuntimeException.class);

        // 트랜잭션 롤백 → afterCompletion(ROLLED_BACK) → stockService.increase() 호출
        // Redis 재고가 초기값으로 복구되어야 한다.
        assertThat(stockService.getStock(testProduct.getId())).isEqualTo(100L);
    }
}
