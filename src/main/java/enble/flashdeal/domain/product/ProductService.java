package enble.flashdeal.domain.product;

import enble.flashdeal.domain.product.dto.ProductCreateRequest;
import enble.flashdeal.domain.product.dto.ProductResponse;
import enble.flashdeal.global.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = Product.create(
                request.name(),
                request.price(),
                request.stockQuantity(),
                request.saleStartAt()
        );
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);
        return ProductResponse.from(product);
    }
}
