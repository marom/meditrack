package com.marom.meditrack.service;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepo;
    @Mock
    private DoctorRepository doctorRepo;
    @Mock
    private PatientRepository patientRepo;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void should_throwResourceNotFoundException_when_patientNotFoundOnBook() {
        // Arrange
        when(patientRepo.findById(1L)).thenReturn(Optional.empty());
        AppointmentBookRequest request = bookRequest();

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_doctorNotFoundOnBook() {
        // Arrange
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient(1L)));
        when(doctorRepo.findById(2L)).thenReturn(Optional.empty());
        AppointmentBookRequest request = bookRequest();

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_throwBusinessRuleException_when_noSlotsAvailableOnBook() {
        // Arrange
        Doctor doctor = doctor(2L, 1, new BigDecimal("120.00"));
        LocalDate scheduled = LocalDate.parse("2026-09-01");
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient(1L)));
        when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepo.countByDoctorAndScheduledDateAndStatusNot(doctor, scheduled, AppointmentStatus.CANCELLED))
                .thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(bookRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No slots available");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_saveRequestedAppointmentWithPendingPayment_when_slotAvailableOnBook() {
        // Arrange
        Doctor doctor = doctor(2L, 5, new BigDecimal("120.00"));
        LocalDate scheduled = LocalDate.parse("2026-09-01");
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient(1L)));
        when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepo.countByDoctorAndScheduledDateAndStatusNot(doctor, scheduled, AppointmentStatus.CANCELLED))
                .thenReturn(0L);
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.book(bookRequest());

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(result.getAppointmentNo()).startsWith("APT-");
        assertThat(result.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(result.getPayment()).isNotNull();
        assertThat(result.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getPayment().getAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void should_throwResourceNotFoundException_when_appointmentNotFoundOnFindById() {
        // Arrange
        when(appointmentRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment not found");
    }

    @Test
    void should_throwResourceNotFoundException_when_appointmentNotFoundOnCancel() {
        // Arrange
        when(appointmentRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_setStatusCancelledAndRefundPayment_when_cancellingRequestedAppointment() {
        // Arrange
        Appointment appointment = requestedAppointment(5L);
        Payment payment = appointment.getPayment();
        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.cancel(5L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void should_throwBusinessRuleException_when_cancellingAnAlreadyCompletedAppointment() {
        // Arrange
        Appointment appointment = requestedAppointment(5L);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancel(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot move appointment");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_appointmentNotFoundOnComplete() {
        // Arrange
        when(appointmentRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.complete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_setStatusCompletedAndMarkPaymentPaid_when_completingRequestedAppointment() {
        // Arrange
        Appointment appointment = requestedAppointment(5L);
        Payment payment = appointment.getPayment();
        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse result = appointmentService.complete(5L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void should_throwBusinessRuleException_when_completingAnAlreadyCancelledAppointment() {
        // Arrange
        Appointment appointment = requestedAppointment(5L);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.complete(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot move appointment");
        verify(appointmentRepo, never()).save(any());
    }

    private AppointmentBookRequest bookRequest() {
        return AppointmentBookRequest.builder()
                .patientId(1L).doctorId(2L).scheduledDate(LocalDate.parse("2026-09-01")).build();
    }

    private Patient patient(Long id) {
        Patient p = new Patient();
        p.setId(id);
        return p;
    }

    private Doctor doctor(Long id, int capacity, BigDecimal fee) {
        Doctor d = new Doctor();
        d.setId(id);
        d.setDailySlotCapacity(capacity);
        d.setConsultationFee(fee);
        return d;
    }

    private Appointment requestedAppointment(Long id) {
        Appointment a = new Appointment();
        a.setAppointmentId(id);
        a.setStatus(AppointmentStatus.REQUESTED);
        Payment payment = new Payment();
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("120.00"));
        payment.setAppointment(a);
        a.setPayment(payment);
        return a;
    }
}
