package com.koperasi.loan.event;

public final class LoanEventType {

    public static final String APPLICATION_CREATED = "loan.application.created";
    public static final String SCORING_REQUESTED = "loan.scoring.requested";
    public static final String APPLICATION_UNDER_REVIEW = "loan.application.under_review";
    public static final String APPLICATION_APPROVED = "loan.application.approved";
    public static final String APPLICATION_REJECTED = "loan.application.rejected";
    public static final String APPLICATION_DISBURSED = "loan.application.disbursed";
    public static final String APPLICATION_ACTIVE = "loan.application.active";
    public static final String APPLICATION_COMPLETED = "loan.application.completed";
    public static final String INSTALLMENT_OVERDUE = "loan.installment.overdue";
    public static final String LOAN_DEFAULTED = "loan.defaulted";

    private LoanEventType() {
    }
}
