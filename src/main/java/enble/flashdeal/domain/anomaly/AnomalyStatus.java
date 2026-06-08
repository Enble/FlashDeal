package enble.flashdeal.domain.anomaly;

public enum AnomalyStatus {
    PENDING,          // Kafka 이벤트 발행 후 Python Agent 처리 대기
    ANALYZED,         // Python Agent 분석 완료
    ANALYSIS_FAILED   // Python Agent 분석 실패
}
