package enble.flashdeal.global.config;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            return;
        }

        List<Member> members = memberRepository.saveAll(List.of(
                Member.create("테스트유저1", "user1@test.com"),
                Member.create("테스트유저2", "user2@test.com"),
                Member.create("테스트유저3", "user3@test.com")
        ));

        // JMeter 부하 테스트용 상품: 재고 100개, 판매 즉시 시작
        productRepository.save(
                Product.create("플래시딜 상품 A", 10_000, 100, LocalDateTime.now().minusMinutes(1))
        );

        log.info("테스트 데이터 초기화 완료 — 회원 {}명, 상품 1개", members.size());
    }
}
