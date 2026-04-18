package enble.flashdeal.domain.coupon;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 4 — Redisson 분산 락 적용 후 정합성 검증.
 *
 * 200명이 동시에 100장 한도 쿠폰을 요청해도
 * 발급 건수가 정확히 100건임을 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class CouponLockConcurrencyTest {

    @Autowired private CouponService couponService;
    @Autowired private CouponRepository couponRepository;
    @Autowired private CouponIssuanceRepository couponIssuanceRepository;
    @Autowired private MemberRepository memberRepository;

    private static final int TOTAL_QUANTITY = 100;
    private static final int THREAD_COUNT = 200;

    private Coupon coupon;
    private List<Member> members;

    @BeforeEach
    void setUp() {
        couponIssuanceRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();

        coupon = couponRepository.save(Coupon.create("플래시딜 쿠폰", TOTAL_QUANTITY));

        members = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            members.add(memberRepository.save(Member.create("유저" + i, "user" + i + "@test.com")));
        }
    }

    @AfterEach
    void tearDown() {
        couponIssuanceRepository.deleteAll();
        couponRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("Redisson 분산 락 적용 시 200명 동시 요청에서 발급 건수가 정확히 100건이다.")
    void withLock_exactIssuance() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long memberId = members.get(i).getId();
            executor.submit(() -> {
                try {
                    couponService.issueWithLock(coupon.getId(), memberId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long issuanceCount = couponIssuanceRepository.count();

        System.out.println("=== Phase 4 Redisson 분산 락 적용 — 정합성 검증 ===");
        System.out.println("쿠폰 발급 한도  : " + TOTAL_QUANTITY);
        System.out.println("총 요청 수      : " + THREAD_COUNT);
        System.out.println("성공 건수       : " + successCount.get());
        System.out.println("실패 건수       : " + failCount.get());
        System.out.println("DB 발급 건수    : " + issuanceCount);

        assertThat(issuanceCount).isEqualTo(TOTAL_QUANTITY);
        assertThat(successCount.get()).isEqualTo(TOTAL_QUANTITY);
    }
}
