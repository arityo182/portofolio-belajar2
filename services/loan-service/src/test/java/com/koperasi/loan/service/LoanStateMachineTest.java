package com.koperasi.loan.service;

import com.koperasi.loan.domain.enums.LoanStatus;
import com.koperasi.loan.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanStateMachineTest {

    private final LoanStateMachine stateMachine = new LoanStateMachine();

    @Test
    void canTransition_allValidTransitions_returnsTrue() {
        assertThat(stateMachine.canTransition(LoanStatus.PENDING, LoanStatus.SCORING)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.PENDING, LoanStatus.UNDER_REVIEW)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.SCORING, LoanStatus.UNDER_REVIEW)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.UNDER_REVIEW, LoanStatus.APPROVED)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.UNDER_REVIEW, LoanStatus.REJECTED)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.APPROVED, LoanStatus.DISBURSED)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.DISBURSED, LoanStatus.ACTIVE)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.DISBURSED, LoanStatus.DEFAULT)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.ACTIVE, LoanStatus.COMPLETED)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.ACTIVE, LoanStatus.DEFAULT)).isTrue();
        assertThat(stateMachine.canTransition(LoanStatus.DEFAULT, LoanStatus.COMPLETED)).isTrue();
    }

    @Test
    void canTransition_invalidTransitions_returnsFalse() {
        assertThat(stateMachine.canTransition(LoanStatus.PENDING, LoanStatus.APPROVED)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.PENDING, LoanStatus.REJECTED)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.SCORING, LoanStatus.APPROVED)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.UNDER_REVIEW, LoanStatus.DISBURSED)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.APPROVED, LoanStatus.ACTIVE)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.DISBURSED, LoanStatus.COMPLETED)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.REJECTED, LoanStatus.PENDING)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.COMPLETED, LoanStatus.ACTIVE)).isFalse();
        assertThat(stateMachine.canTransition(LoanStatus.DEFAULT, LoanStatus.ACTIVE)).isFalse();
    }

    @Test
    void validateTransition_withInvalidTransition_throwsInvalidStateTransitionException() {
        assertThatThrownBy(() -> stateMachine.validateTransition(LoanStatus.PENDING, LoanStatus.APPROVED))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("PENDING")
                .hasMessageContaining("APPROVED");
    }

    @Test
    void validateTransition_withNullStatuses_throwsInvalidStateTransitionException() {
        assertThatThrownBy(() -> stateMachine.validateTransition(null, LoanStatus.APPROVED))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> stateMachine.validateTransition(LoanStatus.PENDING, null))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void allowedTransitionsFrom_returnsImmutableSetForStatus() {
        assertThat(stateMachine.allowedTransitionsFrom(LoanStatus.UNDER_REVIEW))
                .containsExactlyInAnyOrder(LoanStatus.APPROVED, LoanStatus.REJECTED);
        assertThat(stateMachine.allowedTransitionsFrom(LoanStatus.REJECTED)).isEmpty();
    }
}
