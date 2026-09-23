package com.marketplace.payment;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public record DemoPaymentPrices(BigDecimal jobPostingFee, BigDecimal sessionBookingFee, String currency) {
    public DemoPaymentPrices(@Value("${app.demo-payment.job-posting-fee}") BigDecimal jobPostingFee,
        @Value("${app.demo-payment.session-booking-fee}") BigDecimal sessionBookingFee,
        @Value("${app.demo-payment.currency}") String currency) {
        if(jobPostingFee.signum() <= 0 || sessionBookingFee.signum() <= 0 || !currency.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("Invalid demo payment prices");
        this.jobPostingFee=jobPostingFee; this.sessionBookingFee=sessionBookingFee; this.currency=currency;
    }
}
