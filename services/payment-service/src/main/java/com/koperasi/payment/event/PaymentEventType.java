package com.koperasi.payment.event;

public final class PaymentEventType {

    public static final String DISBURSEMENT_INITIATED = "payment.disbursement.initiated";
    public static final String DISBURSEMENT_COMPLETED = "payment.disbursement.completed";
    public static final String DISBURSEMENT_FAILED = "payment.disbursement.failed";
    public static final String INSTALLMENT_RECEIVED = "payment.installment.received";
    public static final String INSTALLMENT_FAILED = "payment.installment.failed";

    private PaymentEventType() {
    }
}
