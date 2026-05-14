package enble.flashdeal.domain.anomaly;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AnomalyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long triggerOrderId;

    @Column(nullable = false, length = 500)
    private String detectedReason;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalyStatus status;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime detectedAt;

    public static AnomalyReport create(Long memberId, Long triggerOrderId, String detectedReason,
                                       String aiSummary, AnomalyStatus status) {
        AnomalyReport report = new AnomalyReport();
        report.memberId = memberId;
        report.triggerOrderId = triggerOrderId;
        report.detectedReason = detectedReason;
        report.aiSummary = aiSummary;
        report.status = status;
        return report;
    }
}
