package enble.flashdeal.global.exception;

public class SaleNotStartedException extends BusinessException {

    public SaleNotStartedException() {
        super(ErrorCode.SALE_NOT_STARTED);
    }
}
