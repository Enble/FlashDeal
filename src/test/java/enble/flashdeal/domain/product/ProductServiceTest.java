package enble.flashdeal.domain.product;

import enble.flashdeal.domain.product.dto.ProductCreateRequest;
import enble.flashdeal.domain.product.dto.ProductResponse;
import enble.flashdeal.global.exception.ProductNotFoundException;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Mock
    private ProductRepository productRepository;

    @Test
    @DisplayName("상품을 등록한다.")
    void createProduct() {
        LocalDateTime saleStartAt = LocalDateTime.now().plusDays(1);
        ProductCreateRequest request = new ProductCreateRequest("상품 A", 10_000, 100, saleStartAt);
        Product product = Product.create("상품 A", 10_000, 100, saleStartAt);

        given(productRepository.save(any())).willReturn(product);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.name()).isEqualTo("상품 A");
        assertThat(response.price()).isEqualTo(10_000);
        assertThat(response.stockQuantity()).isEqualTo(100);
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 예외가 발생한다.")
    void getProduct_notFound() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
