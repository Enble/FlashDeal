package enble.flashdeal.global.config;

import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.domain.product.Product;
import enble.flashdeal.domain.product.ProductRepository;
import enble.flashdeal.domain.product.StockService;
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
    private final StockService stockService;

    private static final String PRODUCT_NAME = "플래시딜 상품 A";
    private static final int INITIAL_STOCK = 10_000;

    @Override
    public void run(String... args) {
        initMembers();
        Product product = initProduct();
        initRedisStock(product);
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

    private Product initProduct() {
        return productRepository.findByName(PRODUCT_NAME).map(product -> {
            product.resetStock(INITIAL_STOCK);
            productRepository.save(product);
            log.info("상품 재고 리셋 완료 — '{}' DB {}개", PRODUCT_NAME, INITIAL_STOCK);
            return product;
        }).orElseGet(() -> {
            Product product = productRepository.save(
                    Product.create(PRODUCT_NAME, 10_000, INITIAL_STOCK, LocalDateTime.now().minusMinutes(1))
            );
            log.info("상품 데이터 초기화 완료 — '{}' {}개", PRODUCT_NAME, INITIAL_STOCK);
            return product;
        });
    }

    private void initRedisStock(Product product) {
        // 앱 재기동 시마다 Redis 재고를 초기값으로 덮어쓴다 (테스트/데모 환경 의도).
        //
        // product.stockQuantity(DB)는 상품 생성 시 설정된 초기 할당량을 의미한다.
        // 판매 중 실시간 재고는 Redis가 단독으로 관리하므로 DB 컬럼은 이후 업데이트되지 않는다.
        // Redis가 재고의 단일 진실 공급원(source of truth)이다.
        //
        // AOF 활성화 시 Redis 프로세스 크래시 → 자동 재시작 구간에서는 AOF가 재고를 복구한다.
        // 단, 앱 자체가 재시작되면 이 메서드가 다시 실행되어 초기값으로 리셋된다.
        stockService.initStock(product.getId(), INITIAL_STOCK);
        log.info("Redis 재고 초기화 완료 — stock:{} = {}", product.getId(), INITIAL_STOCK);
    }
}
