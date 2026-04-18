package enble.flashdeal.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다."),
    SALE_NOT_STARTED(HttpStatus.BAD_REQUEST, "아직 판매 시작 전입니다."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰을 찾을 수 없습니다."),
    ALREADY_ISSUED(HttpStatus.BAD_REQUEST, "이미 발급된 쿠폰입니다."),
    COUPON_EXHAUSTED(HttpStatus.BAD_REQUEST, "쿠폰이 모두 소진되었습니다.");

    private final HttpStatus status;
    private final String message;
}
