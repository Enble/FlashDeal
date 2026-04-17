package enble.flashdeal.domain.coupon;

import enble.flashdeal.domain.coupon.dto.CouponCreateRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueResponse;
import enble.flashdeal.domain.member.Member;
import enble.flashdeal.domain.member.MemberRepository;
import enble.flashdeal.global.exception.AlreadyIssuedException;
import enble.flashdeal.global.exception.CouponExhaustedException;
import enble.flashdeal.global.exception.CouponNotFoundException;
import enble.flashdeal.global.exception.MemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponIssuanceRepository couponIssuanceRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member member;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        member = Member.create("테스트유저", "test@test.com");
        coupon = Coupon.create("테스트 쿠폰", 100);
    }

    @Test
    @DisplayName("쿠폰 발급에 성공한다.")
    void issue_success() {
        given(couponRepository.findById(anyLong())).willReturn(Optional.of(coupon));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(couponIssuanceRepository.existsByCouponIdAndMemberId(anyLong(), anyLong())).willReturn(false);
        given(couponIssuanceRepository.countByCouponId(anyLong())).willReturn(99L);
        given(couponIssuanceRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        CouponIssueResponse response = couponService.issue(1L, 1L);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("이미 발급된 쿠폰 재요청 시 AlreadyIssuedException이 발생한다.")
    void issue_alreadyIssued() {
        given(couponRepository.findById(anyLong())).willReturn(Optional.of(coupon));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(couponIssuanceRepository.existsByCouponIdAndMemberId(anyLong(), anyLong())).willReturn(true);

        assertThatThrownBy(() -> couponService.issue(1L, 1L))
                .isInstanceOf(AlreadyIssuedException.class);
    }

    @Test
    @DisplayName("수량이 소진된 쿠폰 발급 시 CouponExhaustedException이 발생한다.")
    void issue_exhausted() {
        Coupon exhaustedCoupon = Coupon.create("소진 쿠폰", 1);

        given(couponRepository.findById(anyLong())).willReturn(Optional.of(exhaustedCoupon));
        given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
        given(couponIssuanceRepository.existsByCouponIdAndMemberId(anyLong(), anyLong())).willReturn(false);
        given(couponIssuanceRepository.countByCouponId(anyLong())).willReturn(1L);

        assertThatThrownBy(() -> couponService.issue(1L, 1L))
                .isInstanceOf(CouponExhaustedException.class);
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 시 CouponNotFoundException이 발생한다.")
    void issue_couponNotFound() {
        given(couponRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issue(999L, 1L))
                .isInstanceOf(CouponNotFoundException.class);
    }

    @Test
    @DisplayName("존재하지 않는 회원으로 발급 시 MemberNotFoundException이 발생한다.")
    void issue_memberNotFound() {
        given(couponRepository.findById(anyLong())).willReturn(Optional.of(coupon));
        given(memberRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.issue(1L, 999L))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    @DisplayName("쿠폰 생성에 성공한다.")
    void create_success() {
        Coupon saved = Coupon.create("신규 쿠폰", 50);
        given(couponRepository.save(any())).willReturn(saved);

        couponService.create(new CouponCreateRequest("신규 쿠폰", 50));
    }
}
