package enble.flashdeal.domain.settlement;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "processed_order_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_processed_order_events",
                columnNames = {"order_id", "consumer_group"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProcessedOrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime processedAt;

    public static ProcessedOrderEvent of(Long orderId, String consumerGroup) {
        ProcessedOrderEvent e = new ProcessedOrderEvent();
        e.orderId = orderId;
        e.consumerGroup = consumerGroup;
        return e;
    }
}
