package enble.flashdeal.global.exception;

public class AlreadyIssuedException extends BusinessException {

    public AlreadyIssuedException() {
        super(ErrorCode.ALREADY_ISSUED);
    }
}
