package enble.flashdeal.domain.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @Retryable 동작 검증 — AOP 기반이므로 Spring 컨텍스트가 필요하다.
 *
 * increase()는 DB 롤백 보상 경로에서만 호출된다.
 * 일시적 Redis 오류 시 재시도 후 성공, 또는 최종 실패 시 @Recover로 로그 기록을 확인한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class StockServiceRetryTest {

    @Autowired
    private StockService stockService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = Mockito.mock(ValueOperations.class);

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
    }

    @Test
    @DisplayName("increase() 첫 번째 호출 실패 시 재시도하여 성공한다.")
    void increase_retryOnTransientFailure() {
        given(valueOps.increment(anyString(), anyLong()))
                .willThrow(new RuntimeException("Redis 일시 오류"))
                .willReturn(100L);

        assertThatNoException().isThrownBy(() -> stockService.increase(1L, 1));
        verify(valueOps, times(2)).increment("stock:1", 1L);
    }

    @Test
    @DisplayName("increase() 모든 재시도 실패 시 @Recover가 실행되고 예외가 전파되지 않는다.")
    void increase_allRetriesFailed_recoverSuppressesException() {
        given(valueOps.increment(anyString(), anyLong()))
                .willThrow(new RuntimeException("Redis 다운"));

        // @Recover가 예외를 삼키므로 예외 없이 완료 (누수는 발생하나 서비스 흐름은 유지)
        assertThatNoException().isThrownBy(() -> stockService.increase(1L, 1));
        verify(valueOps, times(3)).increment("stock:1", 1L);  // maxAttempts=3
    }
}
