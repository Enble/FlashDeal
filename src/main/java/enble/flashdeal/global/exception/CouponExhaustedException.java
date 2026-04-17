package enble.flashdeal.global.exception;

public class CouponExhaustedException extends BusinessException {

    public CouponExhaustedException() {
        super(ErrorCode.COUPON_EXHAUSTED);
    }
}
