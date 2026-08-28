package com.marom.meditrack.repo;

import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Feedback;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.support.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackRepositoryIT extends AbstractRepositoryIT {

    private final FeedbackRepository feedbackRepo;

    FeedbackRepositoryIT(TestEntityManager em, FeedbackRepository feedbackRepo) {
        super(em);
        this.feedbackRepo = feedbackRepo;
    }

    @Test
    void should_returnTrue_when_feedbackExistsForThePatientAndAppointment() {
        // Arrange
        Patient patient = em.find(Patient.class, 1L);
        Appointment appointment = persistAppointment(em.find(Doctor.class, 1L), patient);
        persistFeedback(patient, appointment, 5);
        em.flush();

        // Act
        boolean exists = feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(
                patient.getId(), appointment.getAppointmentId());

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void should_returnFalse_when_noFeedbackExistsForThePatientAndAppointment() {
        // Arrange
        Patient patient = em.find(Patient.class, 1L);
        Appointment appointment = persistAppointment(em.find(Doctor.class, 1L), patient);
        em.flush();

        // Act
        boolean exists = feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(
                patient.getId(), appointment.getAppointmentId());

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void should_countOnlyFeedbackWhoseAppointmentBelongsToTheDoctor() {
        // Arrange
        Doctor doctor = em.find(Doctor.class, 1L);
        Doctor otherDoctor = em.find(Doctor.class, 2L);
        Patient p1 = em.find(Patient.class, 1L);
        Patient p2 = em.find(Patient.class, 2L);
        persistFeedback(p1, persistAppointment(doctor, p1), 5);
        persistFeedback(p2, persistAppointment(doctor, p2), 3);
        persistFeedback(p1, persistAppointment(otherDoctor, p1), 4);
        em.flush();

        // Act
        long count = feedbackRepo.countByAppointment_Doctor_Id(doctor.getId());

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_returnMeanRating_when_theDoctorHasFeedback() {
        // Arrange
        Doctor doctor = em.find(Doctor.class, 3L);
        Patient p1 = em.find(Patient.class, 1L);
        Patient p2 = em.find(Patient.class, 2L);
        persistFeedback(p1, persistAppointment(doctor, p1), 5);
        persistFeedback(p2, persistAppointment(doctor, p2), 2);
        em.flush();

        // Act
        Double average = feedbackRepo.averageRatingForDoctor(doctor.getId());

        // Assert
        assertThat(average).isEqualTo(3.5);
    }

    @Test
    void should_returnNull_when_theDoctorHasNoFeedback() {
        // Arrange
        Doctor doctor = em.find(Doctor.class, 4L);

        // Act
        Double average = feedbackRepo.averageRatingForDoctor(doctor.getId());

        // Assert
        assertThat(average).isNull();
    }

    private Appointment persistAppointment(Doctor doctor, Patient patient) {
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setDoctor(doctor);
        a.setPatient(patient);
        a.setStatus(AppointmentStatus.COMPLETED);
        a.setScheduledDate(LocalDate.parse("2026-10-01"));
        a.setTotalAmount(BigDecimal.ZERO);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return em.persist(a);
    }

    private void persistFeedback(Patient patient, Appointment appointment, int rating) {
        Feedback f = new Feedback();
        f.setPatient(patient);
        f.setAppointment(appointment);
        f.setRating(rating);
        f.setCreatedAt(LocalDateTime.now());
        em.persist(f);
    }
}
