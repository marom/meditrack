package com.marom.meditrack.repo;

import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Slots consumed by a doctor on a date. Cancelled appointments release their
     * slot, so they are excluded from the capacity check.
     */
    long countByDoctorAndScheduledDateAndStatusNot(Doctor doctor, LocalDate scheduledDate, AppointmentStatus status);
}
