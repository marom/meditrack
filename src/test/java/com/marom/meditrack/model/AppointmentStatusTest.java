package com.marom.meditrack.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentStatusTest {

    @Test
    void should_allowRequestedToComplete_when_checkingTransition() {
        // Act
        boolean allowed = AppointmentStatus.REQUESTED.canTransitionTo(AppointmentStatus.COMPLETED);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_allowRequestedToCancel_when_checkingTransition() {
        // Act
        boolean allowed = AppointmentStatus.REQUESTED.canTransitionTo(AppointmentStatus.CANCELLED);

        // Assert
        assertThat(allowed).isTrue();
    }

    @Test
    void should_rejectLeavingTerminalStates_when_checkingTransition() {
        // Assert
        assertThat(AppointmentStatus.COMPLETED.canTransitionTo(AppointmentStatus.CANCELLED)).isFalse();
        assertThat(AppointmentStatus.CANCELLED.canTransitionTo(AppointmentStatus.COMPLETED)).isFalse();
        assertThat(AppointmentStatus.COMPLETED.canTransitionTo(AppointmentStatus.REQUESTED)).isFalse();
    }

    @Test
    void should_rejectTransitionToSameState_when_checkingTransition() {
        // Assert
        assertThat(AppointmentStatus.REQUESTED.canTransitionTo(AppointmentStatus.REQUESTED)).isFalse();
    }
}
