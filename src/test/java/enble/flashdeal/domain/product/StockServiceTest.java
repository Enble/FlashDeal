package enble.flashdeal.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private static final Long PRODUCT_ID = 1L;
    private static final String STOCK_KEY = "stock:1";

    // ---- decrease (Lua script) ----

    @Test
    @DisplayName("재고가 충분하면 Lua script DECR 후 true를 반환한다.")
    void decrease_success() {
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .willReturn(9L);

        assertThat(stockService.decrease(PRODUCT_ID, 1)).isTrue();
    }

    @Test
    @DisplayName("DECR 후 재고가 정확히 0이 되면 true를 반환한다.")
    void decrease_exactlyZero() {
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .willReturn(0L);

        assertThat(stockService.decrease(PRODUCT_ID, 1)).isTrue();
    }

    @Test
    @DisplayName("재고 부족 시 Lua script가 -1을 반환하면 false를 반환한다.")
    void decrease_outOfStock() {
        // Lua script가 내부에서 INCRBY 보상까지 처리하고 -1을 반환
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .willReturn(-1L);

        assertThat(stockService.decrease(PRODUCT_ID, 1)).isFalse();
    }

    @Test
    @DisplayName("Lua script 결과가 null이면 false를 반환한다.")
    void decrease_nullResult() {
        given(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .willReturn(null);

        assertThat(stockService.decrease(PRODUCT_ID, 1)).isFalse();
    }

    // ---- increase ----

    @Test
    @DisplayName("재고를 증가시킨다.")
    void increase() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        stockService.increase(PRODUCT_ID, 1);

        verify(valueOps).increment(STOCK_KEY, 1);
    }

    // ---- getStock ----

    @Test
    @DisplayName("재고 키가 있으면 파싱된 값을 반환한다.")
    void getStock_keyExists() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(STOCK_KEY)).willReturn("42");

        assertThat(stockService.getStock(PRODUCT_ID)).isEqualTo(42L);
    }

    @Test
    @DisplayName("재고 키가 없으면 0을 반환한다.")
    void getStock_keyMissing() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(STOCK_KEY)).willReturn(null);

        assertThat(stockService.getStock(PRODUCT_ID)).isZero();
    }
}
