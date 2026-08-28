package com.marom.meditrack.service;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
import com.marom.meditrack.dto.AppointmentServiceResponse;
import com.marom.meditrack.dto.PaymentResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.model.Payment;
import com.marom.meditrack.model.PaymentStatus;
import com.marom.meditrack.repo.AppointmentRepository;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    /** Placeholder method for the auto-created, not-yet-settled payment. */
    private static final String UNSETTLED_METHOD = "PENDING";

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> findAll() {
        return appointmentRepo.findAll().stream()
                .map(AppointmentService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        return toResponse(require(id));
    }

    public AppointmentResponse book(AppointmentBookRequest request) {
        Patient patient = patientRepo.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.getPatientId()));
        Doctor doctor = doctorRepo.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.getDoctorId()));

        LocalDate scheduled = request.getScheduledDate();
        long booked = appointmentRepo.countByDoctorAndScheduledDateAndStatusNot(
                doctor, scheduled, AppointmentStatus.CANCELLED);
        if (booked >= doctor.getDailySlotCapacity()) {
            throw new BusinessRuleException("No slots available for this doctor on " + scheduled);
        }

        LocalDateTime now = LocalDateTime.now();
        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setStatus(AppointmentStatus.REQUESTED);
        a.setScheduledDate(scheduled);
        a.setTotalAmount(doctor.getConsultationFee());
        a.setCreatedAt(now);
        a.setUpdatedAt(now);

        Payment payment = new Payment();
        payment.setAppointment(a);
        payment.setPaymentMethod(UNSETTLED_METHOD);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setAmount(doctor.getConsultationFee());
        payment.setCreatedAt(now);
        a.setPayment(payment);

        return toResponse(appointmentRepo.save(a));
    }

    public AppointmentResponse cancel(Long id) {
        Appointment a = require(id);
        transition(a, AppointmentStatus.CANCELLED);

        Payment payment = a.getPayment();
        if (payment != null && payment.getPaymentStatus() != PaymentStatus.REFUNDED) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        return toResponse(appointmentRepo.save(a));
    }

    public AppointmentResponse complete(Long id) {
        Appointment a = require(id);
        transition(a, AppointmentStatus.COMPLETED);

        Payment payment = a.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.PENDING) {
            payment.setPaymentStatus(PaymentStatus.PAID);
        }
        return toResponse(appointmentRepo.save(a));
    }

    private Appointment require(Long id) {
        return appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }

    private void transition(Appointment a, AppointmentStatus target) {
        if (!a.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Cannot move appointment " + a.getAppointmentId() + " from " + a.getStatus() + " to " + target);
        }
        a.setStatus(target);
        a.setUpdatedAt(LocalDateTime.now());
    }

    static AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .appointmentId(a.getAppointmentId())
                .appointmentNo(a.getAppointmentNo())
                .patient(a.getPatient() != null ? PatientService.toResponse(a.getPatient()) : null)
                .doctor(a.getDoctor() != null ? DoctorService.toResponse(a.getDoctor()) : null)
                .status(a.getStatus())
                .scheduledDate(a.getScheduledDate())
                .totalAmount(a.getTotalAmount())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .payment(toPaymentResponse(a.getPayment()))
                .services(a.getServices().stream()
                        .map(s -> AppointmentServiceResponse.builder()
                                .id(s.getId())
                                .serviceName(s.getServiceName())
                                .quantity(s.getQuantity())
                                .unitPrice(s.getUnitPrice())
                                .subtotal(s.getSubtotal())
                                .build())
                        .toList())
                .build();
    }

    private static PaymentResponse toPaymentResponse(Payment p) {
        if (p == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(p.getId())
                .paymentMethod(p.getPaymentMethod())
                .paymentStatus(p.getPaymentStatus())
                .amount(p.getAmount())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
