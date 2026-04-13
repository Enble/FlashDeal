package enble.flashdeal.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stockQuantity;

    @Column(nullable = false)
    private LocalDateTime saleStartAt;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Product create(String name, int price, int stockQuantity, LocalDateTime saleStartAt) {
        Product product = new Product();
        product.name = name;
        product.price = price;
        product.stockQuantity = stockQuantity;
        product.saleStartAt = saleStartAt;
        return product;
    }

    public boolean isSaleStarted() {
        return !LocalDateTime.now().isBefore(saleStartAt);
    }

    // 의도적으로 동시성 제어 없이 구현 (Phase 1 - 오버셀 재현용)
    public void decreaseStock(int quantity) {
        this.stockQuantity -= quantity;
    }
}
