package enble.flashdeal.global.exception;

public class CouponNotFoundException extends BusinessException {

    public CouponNotFoundException() {
        super(ErrorCode.COUPON_NOT_FOUND);
    }
}
