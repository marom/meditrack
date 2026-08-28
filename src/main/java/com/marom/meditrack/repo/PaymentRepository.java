package com.marom.meditrack.repo;

import com.marom.meditrack.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByAppointment_AppointmentId(Long appointmentId);
}
