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

    private static final String PRODUCT_NAME = "플래시딜 상품 A";
    private static final int INITIAL_STOCK = 10_000;

    @Override
    public void run(String... args) {
        initMembers();
        initProduct();
    }

    private void initMembers() {
        if (memberRepository.count() > 0) {
            log.info("회원 데이터 이미 존재 — 건너뜀");
            return;
        }
        List<Member> members = memberRepository.saveAll(List.of(
                Member.create("테스트유저1", "user1@test.com"),
                Member.create("테스트유저2", "user2@test.com"),
                Member.create("테스트유저3", "user3@test.com")
        ));
        log.info("회원 데이터 초기화 완료 — {}명", members.size());
    }

    private void initProduct() {
        // JMeter 부하 테스트용 상품.
        // 서버 재기동 시마다 재고를 초기값으로 리셋해, 이전 테스트 결과가 다음 측정에 영향을 주지 않도록 한다.
        productRepository.findByName(PRODUCT_NAME).ifPresentOrElse(
                product -> {
                    product.resetStock(INITIAL_STOCK);
                    productRepository.save(product);
                    log.info("상품 재고 리셋 완료 — '{}' → {}개", PRODUCT_NAME, INITIAL_STOCK);
                },
                () -> {
                    productRepository.save(
                            Product.create(PRODUCT_NAME, 10_000, INITIAL_STOCK, LocalDateTime.now().minusMinutes(1))
                    );
                    log.info("상품 데이터 초기화 완료 — '{}' {}개", PRODUCT_NAME, INITIAL_STOCK);
                }
        );
    }
}
