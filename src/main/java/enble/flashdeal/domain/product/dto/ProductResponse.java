package enble.flashdeal.domain.product.dto;

import enble.flashdeal.domain.product.Product;

import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        int price,
        int stockQuantity,
        LocalDateTime saleStartAt,
        LocalDateTime createdAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSaleStartAt(),
                product.getCreatedAt()
        );
    }
}
