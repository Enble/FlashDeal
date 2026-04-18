package enble.flashdeal.domain.coupon;

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
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int totalQuantity;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Coupon create(String name, int totalQuantity) {
        Coupon coupon = new Coupon();
        coupon.name = name;
        coupon.totalQuantity = totalQuantity;
        return coupon;
    }
}
