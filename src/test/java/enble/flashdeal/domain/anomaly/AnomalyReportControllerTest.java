package enble.flashdeal.domain.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import enble.flashdeal.config.RestDocsConfig;
import enble.flashdeal.domain.anomaly.dto.AnomalyReportResponse;
import enble.flashdeal.global.exception.AnomalyReportNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnomalyReportController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(RestDocsConfig.class)
class AnomalyReportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AnomalyReportService anomalyReportService;

    private AnomalyReportResponse sampleReport() {
        return new AnomalyReportResponse(
                1L, 42L, 10L,
                "최근 10분 내 2건 주문 감지 (임계값: 2건)",
                "회원 42번은 10분 내 2건의 주문을 시도했습니다. 플래시 세일 1인 1회 원칙에 위배되는 비정상 패턴입니다.",
                AnomalyStatus.AI_COMPLETED,
                LocalDateTime.of(2026, 5, 14, 12, 0, 0)
        );
    }

    @Test
    @DisplayName("이상 거래 리포트 목록을 조회한다.")
    void findAll() throws Exception {
        given(anomalyReportService.findAll()).willReturn(List.of(sampleReport()));

        mockMvc.perform(get("/admin/anomaly-reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(42))
                .andDo(document("anomaly-report-list",
                        responseFields(
                                fieldWithPath("[].id").description("리포트 ID"),
                                fieldWithPath("[].memberId").description("대상 회원 ID"),
                                fieldWithPath("[].triggerOrderId").description("탐지를 유발한 주문 ID"),
                                fieldWithPath("[].detectedReason").description("룰 기반 탐지 사유"),
                                fieldWithPath("[].aiSummary").description("AI 생성 요약 (AI_FAILED 시 null)").optional(),
                                fieldWithPath("[].status").description("리포트 상태 (AI_COMPLETED / AI_FAILED)"),
                                fieldWithPath("[].detectedAt").description("탐지 시각")
                        )
                ));
    }

    @Test
    @DisplayName("이상 거래 리포트를 단건 조회한다.")
    void findById() throws Exception {
        given(anomalyReportService.findById(1L)).willReturn(sampleReport());

        mockMvc.perform(get("/admin/anomaly-reports/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AI_COMPLETED"))
                .andDo(document("anomaly-report-get",
                        pathParameters(
                                parameterWithName("id").description("리포트 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("리포트 ID"),
                                fieldWithPath("memberId").description("대상 회원 ID"),
                                fieldWithPath("triggerOrderId").description("탐지를 유발한 주문 ID"),
                                fieldWithPath("detectedReason").description("룰 기반 탐지 사유"),
                                fieldWithPath("aiSummary").description("AI 생성 요약").optional(),
                                fieldWithPath("status").description("리포트 상태 (AI_COMPLETED / AI_FAILED)"),
                                fieldWithPath("detectedAt").description("탐지 시각")
                        )
                ));
    }

    @Test
    @DisplayName("존재하지 않는 리포트 조회 시 404를 반환한다.")
    void findById_notFound() throws Exception {
        given(anomalyReportService.findById(eq(999L))).willThrow(new AnomalyReportNotFoundException());

        mockMvc.perform(get("/admin/anomaly-reports/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANOMALY_REPORT_NOT_FOUND"))
                .andDo(document("anomaly-report-not-found",
                        responseFields(
                                fieldWithPath("code").description("에러 코드"),
                                fieldWithPath("message").description("에러 메시지")
                        )
                ));
    }
}
