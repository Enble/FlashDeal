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
import enble.flashdeal.global.exception.SaleNotStartedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private StockService stockService;

    private Member member;
    private Product onSaleProduct;
    private Product notYetOnSaleProduct;

    @BeforeEach
    void setUp() {
        member = Member.create("테스트유저", "user@test.com");
        onSaleProduct = Product.create("판매중 상품", 10_000, 10, LocalDateTime.now().minusHours(1));
        notYetOnSaleProduct = Product.create("판매 전 상품", 10_000, 10, LocalDateTime.now().plusDays(1));
    }

    @Test
    @DisplayName("주문에 성공한다.")
    void placeOrder_success() {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        Order order = Order.create(member, onSaleProduct, 1);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(onSaleProduct));
        given(stockService.decrease(anyLong(), anyInt())).willReturn(true);
        given(orderRepository.save(any())).willReturn(order);

        OrderResponse response = orderService.placeOrder(request);

        assertThat(response.quantity()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("재고 부족 시 OutOfStockException이 발생한다.")
    void placeOrder_outOfStock() {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(onSaleProduct));
        given(stockService.decrease(anyLong(), anyInt())).willReturn(false);

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(OutOfStockException.class);
    }

    @Test
    @DisplayName("판매 시작 전 주문 시 SaleNotStartedException이 발생한다.")
    void placeOrder_saleNotStarted() {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productRepository.findById(1L)).willReturn(Optional.of(notYetOnSaleProduct));

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(SaleNotStartedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 주문 시 MemberNotFoundException이 발생한다.")
    void placeOrder_memberNotFound() {
        OrderCreateRequest request = new OrderCreateRequest(999L, 1L, 1);

        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(MemberNotFoundException.class);
    }
}
