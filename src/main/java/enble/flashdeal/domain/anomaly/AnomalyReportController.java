package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.anomaly.dto.AnomalyReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/anomaly-reports")
@RequiredArgsConstructor
public class AnomalyReportController {

    private final AnomalyReportService anomalyReportService;

    @GetMapping
    public ResponseEntity<List<AnomalyReportResponse>> findAll() {
        return ResponseEntity.ok(anomalyReportService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnomalyReportResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(anomalyReportService.findById(id));
    }
}
