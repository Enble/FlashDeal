package enble.flashdeal.domain.anomaly;

import enble.flashdeal.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiReportGenerator {

    private static final int DETECTION_WINDOW_MINUTES = 10;

    private final ChatClient chatClient;

    public String generate(Long memberId, List<Order> recentOrders) {
        String orderList = recentOrders.stream()
                .map(o -> String.format("  - 주문ID: %d, 상품ID: %d, 수량: %d, 시각: %s",
                        o.getId(), o.getProduct().getId(), o.getQuantity(), o.getCreatedAt()))
                .collect(Collectors.joining("\n"));

        String prompt = String.format("""
                당신은 전자상거래 이상 거래 탐지 시스템입니다.

                아래 회원의 최근 주문 내역을 분석하여 이상 거래 여부와 권장 조치를 한국어로 2~3문장으로 요약하세요.

                회원 ID: %d
                분석 시간 창: 최근 %d분
                주문 횟수: %d건

                주문 내역:
                %s

                플래시 세일 서비스 특성상 1인 1회 구매가 원칙입니다. 짧은 시간 내 반복 구매는 비정상 패턴으로 판단합니다.
                """, memberId, DETECTION_WINDOW_MINUTES, recentOrders.size(), orderList);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
