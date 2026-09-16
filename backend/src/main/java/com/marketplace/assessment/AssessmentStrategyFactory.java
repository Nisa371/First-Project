package com.marketplace.assessment;
import com.marketplace.candidate.CandidateType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class AssessmentStrategyFactory {
    private final TechAssessmentStrategy tech;
    private final VoiceAssessmentStrategy voice;
    public AssessmentStrategy forType(CandidateType type) {
        return switch(type) { case TECH -> tech; case TRADE -> voice; };
    }
}
