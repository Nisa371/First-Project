package com.marketplace.job;
public enum ApplicationStatus {
    APPLIED, UNDER_REVIEW, SHORTLISTED, REJECTED, WITHDRAWN;

    public boolean isTerminal() { return this == REJECTED || this == WITHDRAWN; }

    public boolean canTransitionTo(ApplicationStatus next) {
        return switch (this) {
            case APPLIED -> next == UNDER_REVIEW || next == SHORTLISTED || next == REJECTED;
            case UNDER_REVIEW -> next == SHORTLISTED || next == REJECTED;
            case SHORTLISTED -> next == REJECTED;
            case REJECTED, WITHDRAWN -> false;
        };
    }
}
