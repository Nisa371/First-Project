package com.marketplace.interview;

import com.marketplace.common.persistence.CreatedEntity;
import jakarta.persistence.*;

@Entity @lombok.Getter @lombok.NoArgsConstructor(access=lombok.AccessLevel.PROTECTED)
@Table(name="application_assessment_messages", uniqueConstraints=@UniqueConstraint(
    name="uk_assessment_message_sequence", columnNames={"session_id", "sequence_number"}))
public class AssessmentMessage extends CreatedEntity {
    public enum SenderRole { AI, CANDIDATE }
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="session_id", nullable=false, updatable=false)
    private AssessmentSession assessmentSession;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=16, updatable=false) private SenderRole senderRole;
    @Column(nullable=false, length=AssessmentDtos.MAX_ANSWER_LENGTH, updatable=false) private String content;
    @Column(nullable=false, updatable=false) private int sequenceNumber;
    public AssessmentMessage(AssessmentSession session, SenderRole role, String content, int sequence) {
        this.assessmentSession=session; this.senderRole=role; this.content=content; this.sequenceNumber=sequence;
    }
}
