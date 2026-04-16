package enble.flashdeal.domain.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String STOCK_KEY_PREFIX = "stock:";

    /**
     * DECRBY + 조건부 INCRBY 보상을 단일 원자 블록으로 실행하는 Lua script.
     *
     * 기존 Java 2-command 방식(DECR → 결과 확인 → INCR)은
     * DECR 직후 앱 크래시 시 재고가 음수로 영구 고착되는 문제가 있다.
     * Redis는 Lua script를 단일 스레드에서 중단 없이 실행하므로
     * DECR와 보상 INCR 사이에 중간 상태가 없다.
     *
     * 반환값: -1 = 재고 부족, 0 이상 = 차감 후 잔여 재고
     */
    private static final DefaultRedisScript<Long> DECREASE_STOCK_SCRIPT;

    static {
        DECREASE_STOCK_SCRIPT = new DefaultRedisScript<>();
        DECREASE_STOCK_SCRIPT.setLocation(new ClassPathResource("scripts/stock-decrease.lua"));
        DECREASE_STOCK_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    /**
     * 서버 기동 시 Redis 재고 키를 초기값으로 세팅한다.
     */
    public void initStock(Long productId, int quantity) {
        redisTemplate.opsForValue().set(stockKey(productId), String.valueOf(quantity));
    }

    /**
     * Lua script로 재고를 원자적으로 차감한다.
     *
     * @return 차감 성공 여부
     */
    public boolean decrease(Long productId, int quantity) {
        Long result = redisTemplate.execute(
                DECREASE_STOCK_SCRIPT,
                List.of(stockKey(productId)),
                String.valueOf(quantity)
        );
        return result != null && result >= 0;
    }

    /**
     * 재고를 복구한다. DB 트랜잭션 롤백 시 보상 용도로 사용한다.
     *
     * 일시적인 Redis 오류에 대비해 최대 3회(100ms → 200ms 백오프) 재시도한다.
     * 모든 재시도 실패 시 {@link #recoverIncrease}가 호출된다.
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public void increase(Long productId, int quantity) {
        redisTemplate.opsForValue().increment(stockKey(productId), quantity);
    }

    /**
     * increase() 재시도가 모두 소진됐을 때 호출된다.
     * 이 시점에 재고 누수가 발생하므로 ERROR 로그로 수동 확인을 요청한다.
     */
    @Recover
    public void recoverIncrease(Exception e, Long productId, int quantity) {
        log.error("[재고 복구 최종 실패] stock:{} +{}개 복구 불가. 수동 확인 필요. cause={}",
                productId, quantity, e.getMessage());
    }

    public Long getStock(Long productId) {
        String value = redisTemplate.opsForValue().get(stockKey(productId));
        return value == null ? 0L : Long.parseLong(value);
    }

    private String stockKey(Long productId) {
        return STOCK_KEY_PREFIX + productId;
    }
}
