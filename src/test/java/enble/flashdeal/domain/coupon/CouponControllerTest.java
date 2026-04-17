package enble.flashdeal.domain.coupon;

import com.fasterxml.jackson.databind.ObjectMapper;
import enble.flashdeal.config.RestDocsConfig;
import enble.flashdeal.domain.coupon.dto.CouponCreateRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueResponse;
import enble.flashdeal.global.exception.AlreadyIssuedException;
import enble.flashdeal.global.exception.CouponExhaustedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(RestDocsConfig.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 생성에 성공한다.")
    void create() throws Exception {
        CouponCreateRequest request = new CouponCreateRequest("플래시딜 쿠폰", 100);
        given(couponService.create(any())).willReturn(1L);

        mockMvc.perform(post("/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("coupon-create",
                        requestFields(
                                fieldWithPath("name").description("쿠폰 이름"),
                                fieldWithPath("totalQuantity").description("발급 한도")
                        )
                ));
    }

    @Test
    @DisplayName("쿠폰 발급에 성공한다.")
    void issue() throws Exception {
        CouponIssueRequest request = new CouponIssueRequest(1L);
        CouponIssueResponse response = new CouponIssueResponse(1L, 1L, 1L, LocalDateTime.now());
        given(couponService.issue(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(post("/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andDo(document("coupon-issue",
                        pathParameters(
                                parameterWithName("couponId").description("쿠폰 ID")
                        ),
                        requestFields(
                                fieldWithPath("memberId").description("발급 요청 회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("발급 ID"),
                                fieldWithPath("couponId").description("쿠폰 ID"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("issuedAt").description("발급 시각")
                        )
                ));
    }

    @Test
    @DisplayName("이미 발급된 쿠폰 재요청 시 400을 반환한다.")
    void issue_alreadyIssued() throws Exception {
        given(couponService.issue(anyLong(), anyLong())).willThrow(new AlreadyIssuedException());

        mockMvc.perform(post("/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponIssueRequest(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ALREADY_ISSUED"));
    }

    @Test
    @DisplayName("수량 소진 시 400을 반환한다.")
    void issue_exhausted() throws Exception {
        given(couponService.issue(anyLong(), anyLong())).willThrow(new CouponExhaustedException());

        mockMvc.perform(post("/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CouponIssueRequest(1L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COUPON_EXHAUSTED"));
    }
}
