package com.marom.meditrack.repo;

import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.model.Payment;
import com.marom.meditrack.model.PaymentStatus;
import com.marom.meditrack.support.AbstractRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryIT extends AbstractRepositoryIT {

    private final PaymentRepository paymentRepo;

    PaymentRepositoryIT(TestEntityManager em, PaymentRepository paymentRepo) {
        super(em);
        this.paymentRepo = paymentRepo;
    }

    @Test
    void should_findThePayment_when_lookingUpByAppointmentId() {
        // Arrange
        Appointment appointment = persistAppointmentWithPayment();
        em.flush();
        em.clear();

        // Act
        Optional<Payment> found = paymentRepo.findByAppointment_AppointmentId(appointment.getAppointmentId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(found.get().getAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void should_returnEmpty_when_noPaymentExistsForTheAppointmentId() {
        // Act
        Optional<Payment> found = paymentRepo.findByAppointment_AppointmentId(999_999L);

        // Assert
        assertThat(found).isEmpty();
    }

    private Appointment persistAppointmentWithPayment() {
        Doctor doctor = em.find(Doctor.class, 1L);
        Patient patient = em.find(Patient.class, 1L);

        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setDoctor(doctor);
        a.setPatient(patient);
        a.setStatus(AppointmentStatus.REQUESTED);
        a.setScheduledDate(LocalDate.parse("2026-10-01"));
        a.setTotalAmount(new BigDecimal("600.00"));
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());

        Payment payment = new Payment();
        payment.setAppointment(a);
        payment.setPaymentMethod("PENDING");
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("600.00"));
        payment.setCreatedAt(LocalDateTime.now());
        a.setPayment(payment);

        return em.persist(a);
    }
}
