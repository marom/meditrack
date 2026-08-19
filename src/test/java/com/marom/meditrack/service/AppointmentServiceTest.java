package com.marom.meditrack.service;

import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Patient;
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

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(1L, 2L, "2026-09-01"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_doctorNotFoundOnBook() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(1L);
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(1L, 2L, "2026-09-01"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_throwException_when_noSlotsAvailableOnBook() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(1L);
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        doctor.setDailySlotCapacity(1);
        LocalDate scheduled = LocalDate.parse("2026-09-01");
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepo.countByDoctorAndScheduledDate(doctor, scheduled)).thenReturn(1L);

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.book(1L, 2L, "2026-09-01"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No slots available");
        verify(appointmentRepo, never()).save(any());
    }

    @Test
    void should_saveRequestedAppointment_when_slotAvailableOnBook() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(1L);
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        doctor.setDailySlotCapacity(5);
        doctor.setConsultationFee(new BigDecimal("120.00"));
        LocalDate scheduled = LocalDate.parse("2026-09-01");
        when(patientRepo.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepo.findById(2L)).thenReturn(Optional.of(doctor));
        when(appointmentRepo.countByDoctorAndScheduledDate(doctor, scheduled)).thenReturn(0L);
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Appointment result = appointmentService.book(1L, 2L, "2026-09-01");

        // Assert
        assertThat(result.getStatus()).isEqualTo("REQUESTED");
        assertThat(result.getPatient()).isEqualTo(patient);
        assertThat(result.getDoctor()).isEqualTo(doctor);
        assertThat(result.getScheduledDate()).isEqualTo(scheduled);
        assertThat(result.getTotalAmount()).isEqualTo(doctor.getConsultationFee());
        assertThat(result.getAppointmentNo()).startsWith("APT-");
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
    void should_setStatusCancelled_when_cancellingExistingAppointment() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(5L);
        appointment.setStatus("REQUESTED");
        when(appointmentRepo.findById(5L)).thenReturn(Optional.of(appointment));
        when(appointmentRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Appointment result = appointmentService.cancel(5L);

        // Assert
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        assertThat(result.getUpdatedAt()).isNotNull();
    }
}
