package enble.flashdeal.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY_PREFIX = "stock:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 서버 기동 시 Redis 재고 키를 초기값으로 세팅한다.
     * 이전 테스트로 남은 값을 덮어써서 항상 깨끗한 상태에서 시작하도록 SET을 사용한다.
     */
    public void initStock(Long productId, int quantity) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(quantity));
    }

    /**
     * 재고를 1 차감한다.
     *
     * DECR는 Redis 단일 스레드 명령 실행 모델에 의해 원자적으로 처리된다.
     * 결과가 0 미만이면 재고가 소진된 것이므로 즉시 INCR로 원상복구하고 false를 반환한다.
     *
     * @return 차감 성공 여부
     */
    public boolean decrease(Long productId, int quantity) {
        Long remaining = redisTemplate.opsForValue().decrement(stockKey(productId), quantity);
        if (remaining == null || remaining < 0) {
            redisTemplate.opsForValue().increment(stockKey(productId), quantity);
            return false;
        }
        return true;
    }

    public Long getStock(Long productId) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        return value == null ? 0L : Long.parseLong(value);
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }
}
