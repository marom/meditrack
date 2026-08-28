package com.marom.meditrack.model;

/**
 * Lifecycle of a {@link Payment}. A booking opens a {@link #PENDING} payment;
 * completing the appointment marks it {@link #PAID}; cancelling refunds it to
 * {@link #REFUNDED}.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED
}
