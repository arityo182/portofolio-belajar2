package com.koperasi.loan.service;

import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LoanStateMachine {

    private static final Map<LoanStatus, Set<LoanStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<LoanStatus, Set<LoanStatus>> transitions = new EnumMap<>(LoanStatus.class);
        transitions.put(LoanStatus.PENDING, EnumSet.of(LoanStatus.SCORING, LoanStatus.UNDER_REVIEW));
        transitions.put(LoanStatus.SCORING, EnumSet.of(LoanStatus.UNDER_REVIEW));
        transitions.put(LoanStatus.UNDER_REVIEW, EnumSet.of(LoanStatus.APPROVED, LoanStatus.REJECTED));
        transitions.put(LoanStatus.APPROVED, EnumSet.of(LoanStatus.DISBURSED));
        transitions.put(LoanStatus.DISBURSED, EnumSet.of(LoanStatus.ACTIVE, LoanStatus.DEFAULT));
        transitions.put(LoanStatus.ACTIVE, EnumSet.of(LoanStatus.COMPLETED, LoanStatus.DEFAULT));
        transitions.put(LoanStatus.REJECTED, EnumSet.noneOf(LoanStatus.class));
        transitions.put(LoanStatus.COMPLETED, EnumSet.noneOf(LoanStatus.class));
        // A defaulted loan can still be settled by the borrower.
        transitions.put(LoanStatus.DEFAULT, EnumSet.of(LoanStatus.COMPLETED));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    public void validateTransition(LoanStatus from, LoanStatus to) {
        if (from == null || to == null || !canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }

    public boolean canTransition(LoanStatus from, LoanStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public Set<LoanStatus> allowedTransitionsFrom(LoanStatus from) {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()));
    }
}
