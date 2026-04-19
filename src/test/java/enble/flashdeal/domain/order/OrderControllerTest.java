package enble.flashdeal.domain.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import enble.flashdeal.config.RestDocsConfig;
import enble.flashdeal.domain.order.dto.OrderCreateRequest;
import enble.flashdeal.domain.order.dto.OrderResponse;
import enble.flashdeal.global.exception.OutOfStockException;
import enble.flashdeal.global.exception.SaleNotStartedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(RestDocsConfig.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private OrderService orderService;

    @Test
    @DisplayName("주문에 성공한다.")
    void placeOrder() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        OrderResponse response = new OrderResponse(1L, 1L, 1L, 1, OrderStatus.COMPLETED, LocalDateTime.now());
        given(orderService.placeOrder(any())).willReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(document("order-create",
                        requestFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량")
                        ),
                        responseFields(
                                fieldWithPath("id").description("주문 ID"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량"),
                                fieldWithPath("status").description("주문 상태"),
                                fieldWithPath("createdAt").description("주문 시간")
                        )
                ));
    }

    @Test
    @DisplayName("동기 후처리 버전 — 주문에 성공한다.")
    void placeOrderWithSyncPost() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        OrderResponse response = new OrderResponse(1L, 1L, 1L, 1, OrderStatus.COMPLETED, LocalDateTime.now());
        given(orderService.placeOrderWithSyncPost(any())).willReturn(response);

        mockMvc.perform(post("/orders/sync-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(document("order-sync-post",
                        requestFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량")
                        ),
                        responseFields(
                                fieldWithPath("id").description("주문 ID"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량"),
                                fieldWithPath("status").description("주문 상태"),
                                fieldWithPath("createdAt").description("주문 시간")
                        )
                ));
    }

    @Test
    @DisplayName("비동기 후처리 버전 — 주문에 성공하고 이벤트가 발행된다.")
    void placeOrderWithAsyncPost() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        OrderResponse response = new OrderResponse(1L, 1L, 1L, 1, OrderStatus.COMPLETED, LocalDateTime.now());
        given(orderService.placeOrderWithAsyncPost(any())).willReturn(response);

        mockMvc.perform(post("/orders/async-post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andDo(document("order-async-post",
                        requestFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량")
                        ),
                        responseFields(
                                fieldWithPath("id").description("주문 ID"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("productId").description("상품 ID"),
                                fieldWithPath("quantity").description("주문 수량"),
                                fieldWithPath("status").description("주문 상태"),
                                fieldWithPath("createdAt").description("주문 시간")
                        )
                ));
    }

    @Test
    @DisplayName("재고 부족 시 400을 반환한다.")
    void placeOrder_outOfStock() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        given(orderService.placeOrder(any())).willThrow(new OutOfStockException());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUT_OF_STOCK"));
    }

    @Test
    @DisplayName("판매 시작 전 주문 시 400을 반환한다.")
    void placeOrder_saleNotStarted() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest(1L, 1L, 1);
        given(orderService.placeOrder(any())).willThrow(new SaleNotStartedException());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SALE_NOT_STARTED"));
    }
}
