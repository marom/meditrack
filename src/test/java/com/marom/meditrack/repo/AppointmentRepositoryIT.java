package com.marom.meditrack.repo;

import com.marom.meditrack.model.Appointment;
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
    void should_countOnlyAppointmentsForTheGivenDoctorAndDate() {
        // Arrange
        Doctor doctorA = em.find(Doctor.class, 1L);
        Doctor doctorB = em.find(Doctor.class, 2L);
        Patient patient = em.find(Patient.class, 1L);
        LocalDate target = LocalDate.parse("2026-10-01");
        persistAppointment(doctorA, patient, target);
        persistAppointment(doctorA, patient, target);
        persistAppointment(doctorA, patient, LocalDate.parse("2026-10-02"));
        persistAppointment(doctorB, patient, target);
        em.flush();

        // Act
        long count = appointmentRepo.countByDoctorAndScheduledDate(doctorA, target);

        // Assert
        assertThat(count).isEqualTo(2);
    }

    private void persistAppointment(Doctor doctor, Patient patient, LocalDate scheduledDate) {
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setDoctor(doctor);
        a.setPatient(patient);
        a.setStatus("REQUESTED");
        a.setScheduledDate(scheduledDate);
        a.setTotalAmount(BigDecimal.ZERO);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        em.persist(a);
    }
}
