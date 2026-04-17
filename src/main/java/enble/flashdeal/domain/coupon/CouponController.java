package enble.flashdeal.domain.coupon;

import enble.flashdeal.domain.coupon.dto.CouponCreateRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueRequest;
import enble.flashdeal.domain.coupon.dto.CouponIssueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Long create(@Valid @RequestBody CouponCreateRequest request) {
        return couponService.create(request);
    }

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public CouponIssueResponse issue(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponIssueRequest request) {
        return couponService.issue(couponId, request.memberId());
    }
}
