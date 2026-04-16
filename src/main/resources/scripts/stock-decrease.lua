-- KEYS[1] : stock key (e.g. stock:1)
-- ARGV[1]  : quantity to decrease (string)
--
-- 반환값:
--   -1        : 재고 부족 (INCRBY로 원상복구 완료)
--   0 이상    : 차감 후 잔여 재고
--
-- DECRBY와 보상 INCRBY를 하나의 원자 블록으로 묶는다.
-- Java에서 두 명령을 분리하면 DECRBY 직후 앱 크래시 시 재고가 음수로 남는다.
-- Lua script는 Redis 단일 스레드에서 중단 없이 실행되므로 이 문제가 없다.
local quantity = tonumber(ARGV[1])
local remaining = redis.call('DECRBY', KEYS[1], quantity)
if remaining < 0 then
    redis.call('INCRBY', KEYS[1], quantity)
    return -1
end
return remaining
