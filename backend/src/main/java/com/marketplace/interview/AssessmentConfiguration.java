package com.marketplace.interview;

@org.springframework.stereotype.Component
@org.springframework.boot.context.properties.ConfigurationProperties("app.assessment")
@org.springframework.validation.annotation.Validated
@lombok.Getter @lombok.Setter
public class AssessmentConfiguration {
    @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(10)
    private int maxTurns = 6;
}
