package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.anomaly.dto.AnomalyReportResponse;
import enble.flashdeal.global.exception.AnomalyReportNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnomalyReportService {

    private final AnomalyReportRepository anomalyReportRepository;

    public List<AnomalyReportResponse> findAll() {
        return anomalyReportRepository.findAll().stream()
                .map(AnomalyReportResponse::from)
                .toList();
    }

    public AnomalyReportResponse findById(Long id) {
        AnomalyReport report = anomalyReportRepository.findById(id)
                .orElseThrow(AnomalyReportNotFoundException::new);
        return AnomalyReportResponse.from(report);
    }
}
