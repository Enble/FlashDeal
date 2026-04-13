package enble.flashdeal.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    @DisplayName("판매 시작 시간이 현재 이전이면 판매 중이다.")
    void isSaleStarted_true() {
        Product product = Product.create("상품", 1000, 10, LocalDateTime.now().minusSeconds(1));
        assertThat(product.isSaleStarted()).isTrue();
    }

    @Test
    @DisplayName("판매 시작 시간이 현재 이후이면 판매 전이다.")
    void isSaleStarted_false() {
        Product product = Product.create("상품", 1000, 10, LocalDateTime.now().plusDays(1));
        assertThat(product.isSaleStarted()).isFalse();
    }

    @Test
    @DisplayName("재고를 차감한다.")
    void decreaseStock() {
        Product product = Product.create("상품", 1000, 10, LocalDateTime.now());
        product.decreaseStock(3);
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("동시성 제어 없이 재고를 차감하면 음수가 될 수 있다. (Phase 1 오버셀 재현)")
    void decreaseStock_oversell() {
        Product product = Product.create("상품", 1000, 1, LocalDateTime.now());
        product.decreaseStock(1);
        product.decreaseStock(1); // 오버셀 — 재고가 -1이 됨
        assertThat(product.getStockQuantity()).isEqualTo(-1);
    }
}
