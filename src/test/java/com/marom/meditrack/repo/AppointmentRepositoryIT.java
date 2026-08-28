package com.marom.meditrack.repo;

import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.support.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentRepositoryIT extends AbstractRepositoryIT {

    private final AppointmentRepository appointmentRepo;

    AppointmentRepositoryIT(TestEntityManager em, AppointmentRepository appointmentRepo) {
        super(em);
        this.appointmentRepo = appointmentRepo;
    }

    @Test
    void should_countOnlyActiveAppointmentsForTheGivenDoctorAndDate() {
        // Arrange
        Doctor doctorA = em.find(Doctor.class, 1L);
        Doctor doctorB = em.find(Doctor.class, 2L);
        Patient patient = em.find(Patient.class, 1L);
        LocalDate target = LocalDate.parse("2026-10-01");
        persistAppointment(doctorA, patient, target, AppointmentStatus.REQUESTED);
        persistAppointment(doctorA, patient, target, AppointmentStatus.COMPLETED);
        persistAppointment(doctorA, patient, target, AppointmentStatus.CANCELLED);
        persistAppointment(doctorA, patient, LocalDate.parse("2026-10-02"), AppointmentStatus.REQUESTED);
        persistAppointment(doctorB, patient, target, AppointmentStatus.REQUESTED);
        em.flush();

        // Act — cancelled appointments release their slot and are not counted.
        long count = appointmentRepo.countByDoctorAndScheduledDateAndStatusNot(
                doctorA, target, AppointmentStatus.CANCELLED);

        // Assert
        assertThat(count).isEqualTo(2);
    }

    private void persistAppointment(Doctor doctor, Patient patient, LocalDate scheduledDate, AppointmentStatus status) {
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setDoctor(doctor);
        a.setPatient(patient);
        a.setStatus(status);
        a.setScheduledDate(scheduledDate);
        a.setTotalAmount(BigDecimal.ZERO);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        em.persist(a);
    }
}
