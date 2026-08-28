package com.marom.meditrack.model;

/**
 * Lifecycle of an {@link Appointment}. The machine is one-way: a freshly booked
 * appointment is {@link #REQUESTED} and may move to exactly one terminal state
 * ({@link #COMPLETED} or {@link #CANCELLED}); terminal states never transition.
 */
public enum AppointmentStatus {
    REQUESTED,
    COMPLETED,
    CANCELLED;

    /** Whether this status is allowed to move to {@code target}. */
    public boolean canTransitionTo(AppointmentStatus target) {
        return this == REQUESTED && (target == COMPLETED || target == CANCELLED);
    }
}
