package com.marketplace.placement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import jakarta.validation.constraints.*;
import com.marketplace.common.api.ApiException;

public final class PlacementFeeAgreement {
    private PlacementFeeAgreement() {}
    public record Input(@NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal agreedFirstMonthSalary,
        @NotNull @AssertTrue Boolean agreementAccepted) {}
    public static void waiveReplacement(Placement placement) {
        placement.setPlacementFeeRate(BigDecimal.ZERO.setScale(2));
        placement.setPlacementFeeAmount(BigDecimal.ZERO.setScale(2));
        placement.setTermsVersion("REPLACEMENT_FEE_WAIVED_V1");
    }
    public static void record(Placement placement, Input input, Long employerUserId) {
        if(input==null || !Boolean.TRUE.equals(input.agreementAccepted()) || input.agreedFirstMonthSalary()==null
            || input.agreedFirstMonthSalary().signum()<=0 || input.agreedFirstMonthSalary().scale()>2
            || input.agreedFirstMonthSalary().compareTo(new BigDecimal("999999999999.99"))>0)
            throw new ApiException(400,"FEE_AGREEMENT_REQUIRED","Accept the placement fee agreement and enter a valid first-month salary in BDT (up to two decimal places).");
        if(placement.getFeeAgreementAcceptedAt()!=null) throw new ApiException(409,"FEE_AGREEMENT_EXISTS","This placement already has a fee agreement.");
        placement.setAgreedFirstMonthSalary(input.agreedFirstMonthSalary().setScale(2));
        placement.setPlacementFeeRate(new BigDecimal("0.20"));
        placement.setPlacementFeeAmount(input.agreedFirstMonthSalary().multiply(new BigDecimal("0.20")).setScale(2,RoundingMode.HALF_UP));
        placement.setFeeAgreementAcceptedAt(Instant.now());
        placement.setAcceptedByEmployerUserId(employerUserId);
        placement.setTermsVersion("PLACEMENT_FEE_V2");
    }
}
