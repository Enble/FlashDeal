package enble.flashdeal.domain.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import enble.flashdeal.config.RestDocsConfig;
import enble.flashdeal.domain.product.dto.ProductCreateRequest;
import enble.flashdeal.domain.product.dto.ProductResponse;
import enble.flashdeal.global.exception.ProductNotFoundException;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import(RestDocsConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private final LocalDateTime saleStartAt = LocalDateTime.of(2026, 5, 1, 10, 0, 0);

    @Test
    @DisplayName("상품을 등록한다.")
    void createProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest("플래시딜 상품 A", 10_000, 100, saleStartAt);
        ProductResponse response = new ProductResponse(1L, "플래시딜 상품 A", 10_000, 100, saleStartAt, LocalDateTime.now());

        given(productService.createProduct(any())).willReturn(response);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("플래시딜 상품 A"))
                .andDo(document("product-create",
                        requestFields(
                                fieldWithPath("name").description("상품명"),
                                fieldWithPath("price").description("가격"),
                                fieldWithPath("stockQuantity").description("재고 수량"),
                                fieldWithPath("saleStartAt").description("판매 시작 시간 (ISO 8601)")
                        ),
                        responseFields(
                                fieldWithPath("id").description("상품 ID"),
                                fieldWithPath("name").description("상품명"),
                                fieldWithPath("price").description("가격"),
                                fieldWithPath("stockQuantity").description("재고 수량"),
                                fieldWithPath("saleStartAt").description("판매 시작 시간"),
                                fieldWithPath("createdAt").description("등록 시간")
                        )
                ));
    }

    @Test
    @DisplayName("상품을 단건 조회한다.")
    void getProduct() throws Exception {
        ProductResponse response = new ProductResponse(1L, "플래시딜 상품 A", 10_000, 100, saleStartAt, LocalDateTime.now());

        given(productService.getProduct(1L)).willReturn(response);

        mockMvc.perform(get("/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andDo(document("product-get",
                        pathParameters(
                                parameterWithName("id").description("상품 ID")
                        ),
                        responseFields(
                                fieldWithPath("id").description("상품 ID"),
                                fieldWithPath("name").description("상품명"),
                                fieldWithPath("price").description("가격"),
                                fieldWithPath("stockQuantity").description("재고 수량"),
                                fieldWithPath("saleStartAt").description("판매 시작 시간"),
                                fieldWithPath("createdAt").description("등록 시간")
                        )
                ));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404를 반환한다.")
    void getProduct_notFound() throws Exception {
        given(productService.getProduct(999L)).willThrow(new ProductNotFoundException());

        mockMvc.perform(get("/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }
}
