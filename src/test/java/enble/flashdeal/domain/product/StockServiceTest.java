package enble.flashdeal.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    @DisplayName("재고가 충분하면 DECR 후 true를 반환한다.")
    void decrease_success() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.decrement(STOCK_KEY, 1)).willReturn(9L);

        boolean result = stockService.decrease(PRODUCT_ID, 1);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("DECR 후 재고가 정확히 0이 되면 true를 반환한다.")
    void decrease_exactlyZero() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.decrement(STOCK_KEY, 1)).willReturn(0L);

        boolean result = stockService.decrease(PRODUCT_ID, 1);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("DECR 후 재고가 음수이면 INCR로 복구하고 false를 반환한다.")
    void decrease_outOfStock() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.decrement(STOCK_KEY, 1)).willReturn(-1L);

        boolean result = stockService.decrease(PRODUCT_ID, 1);

        assertThat(result).isFalse();
        verify(valueOps).increment(STOCK_KEY, 1);
    }

    @Test
    @DisplayName("DECR 결과가 null이면 INCR로 복구하고 false를 반환한다.")
    void decrease_nullResult() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.decrement(STOCK_KEY, 1)).willReturn(null);

        boolean result = stockService.decrease(PRODUCT_ID, 1);

        assertThat(result).isFalse();
        verify(valueOps).increment(STOCK_KEY, 1);
    }

    @Test
    @DisplayName("재고 키가 있으면 파싱된 값을 반환한다.")
    void getStock_keyExists() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(STOCK_KEY)).willReturn("42");

        Long stock = stockService.getStock(PRODUCT_ID);

        assertThat(stock).isEqualTo(42L);
    }

    @Test
    @DisplayName("재고 키가 없으면 0을 반환한다.")
    void getStock_keyMissing() {
        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get(STOCK_KEY)).willReturn(null);

        Long stock = stockService.getStock(PRODUCT_ID);

        assertThat(stock).isZero();
    }
}
