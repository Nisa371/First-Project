package com.marketplace.job;

import com.marketplace.common.api.ApiException;
import java.math.BigDecimal;
import java.math.MathContext;
import static java.math.BigDecimal.*;

/** Application merit calculations; experience deliberately has no upper bound of one. */
public final class MeritScoring {
    private MeritScoring() {}

    public static BigDecimal experience(int currentMonths, int expectedMonths) {
        if (currentMonths < 0 || expectedMonths < 0) throw invalid("Experience cannot be negative.");
        if (expectedMonths == 0) return ONE;
        // Integer division implements floor exactly above one, including large month counts.
        if (currentMonths > expectedMonths) return valueOf(currentMonths / expectedMonths);
        return valueOf(currentMonths).divide(valueOf(expectedMonths), MathContext.DECIMAL128);
    }

    public static BigDecimal finalScore(BigDecimal experience, BigDecimal assessment,
            BigDecimal cv, BigDecimal portfolio, JobDtos.EvaluationWeights weights) {
        return experience.multiply(weights.experienceWeight())
            .add(effective(assessment).multiply(weights.assessmentWeight()))
            .add(effective(cv).multiply(weights.cvWeight()))
            .add(effective(portfolio).multiply(weights.portfolioWeight()));
    }

    public static String evaluationStatus(BigDecimal cv, BigDecimal portfolio, BigDecimal assessment) {
        int count=(cv == null ? 0 : 1)+(portfolio == null ? 0 : 1)+(assessment == null ? 0 : 1);
        return count == 0 ? "NOT_EVALUATED" : count == 3 ? "EVALUATED" : "PARTIALLY_EVALUATED";
    }

    public static void unitValue(BigDecimal value) {
        if (value == null || value.compareTo(ZERO) < 0 || value.compareTo(ONE) > 0)
            throw invalid("Scores and weights must be numbers between 0 and 1.");
        // Persist chosen values exactly rather than silently rounding them at the database boundary.
        if (value.stripTrailingZeros().scale() > 16)
            throw invalid("Use at most 16 decimal places.");
    }

    private static BigDecimal effective(BigDecimal score) { return score == null ? ZERO : score; }
    private static ApiException invalid(String message) { return new ApiException(400,"VALIDATION_ERROR",message); }
}
