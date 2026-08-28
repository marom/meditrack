package com.marom.meditrack.service;

import com.marom.meditrack.dto.DoctorAverageRatingResponse;
import com.marom.meditrack.dto.FeedbackRequest;
import com.marom.meditrack.dto.FeedbackResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Feedback;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.repo.AppointmentRepository;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.FeedbackRepository;
import com.marom.meditrack.repo.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepo;
    @Mock
    private AppointmentRepository appointmentRepo;
    @Mock
    private PatientRepository patientRepo;
    @Mock
    private DoctorRepository doctorRepo;

    @InjectMocks
    private FeedbackService feedbackService;

    private Appointment completedAppointment(Long appointmentId, Long patientId) {
        Patient patient = new Patient();
        patient.setId(patientId);
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(appointmentId);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setPatient(patient);
        return appointment;
    }

    @Test
    void should_throwResourceNotFoundException_when_appointmentNotFoundOnSubmit() {
        // Arrange
        when(appointmentRepo.findById(1L)).thenReturn(Optional.empty());
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(5).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment not found");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwBusinessRuleException_when_appointmentNotCompletedOnSubmit() {
        // Arrange
        Appointment appointment = new Appointment();
        appointment.setAppointmentId(1L);
        appointment.setStatus(AppointmentStatus.REQUESTED);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(5).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_patientNotFoundOnSubmit() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(patientRepo.findById(1L)).thenReturn(Optional.empty());
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(5).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient not found");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwBusinessRuleException_when_patientDoesNotMatchAppointmentPatientOnSubmit() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        Patient otherPatient = new Patient();
        otherPatient.setId(2L);
        when(patientRepo.findById(2L)).thenReturn(Optional.of(otherPatient));
        FeedbackRequest request = FeedbackRequest.builder().patientId(2L).appointmentId(1L).rating(5).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not match");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwDuplicateResourceException_when_feedbackAlreadyExistsOnSubmit() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(patientRepo.findById(1L)).thenReturn(Optional.of(appointment.getPatient()));
        when(feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(1L, 1L)).thenReturn(true);
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(5).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already submitted");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwBusinessRuleException_when_ratingBelowRangeOnSubmit() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(patientRepo.findById(1L)).thenReturn(Optional.of(appointment.getPatient()));
        when(feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(1L, 1L)).thenReturn(false);
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(0).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_throwBusinessRuleException_when_ratingAboveRangeOnSubmit() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(patientRepo.findById(1L)).thenReturn(Optional.of(appointment.getPatient()));
        when(feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(1L, 1L)).thenReturn(false);
        FeedbackRequest request = FeedbackRequest.builder().patientId(1L).appointmentId(1L).rating(6).build();

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.submit(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
        verify(feedbackRepo, never()).save(any());
    }

    @Test
    void should_saveFeedback_when_validSubmission() {
        // Arrange
        Appointment appointment = completedAppointment(1L, 1L);
        when(appointmentRepo.findById(1L)).thenReturn(Optional.of(appointment));
        when(patientRepo.findById(1L)).thenReturn(Optional.of(appointment.getPatient()));
        when(feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(1L, 1L)).thenReturn(false);
        when(feedbackRepo.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        FeedbackRequest request = FeedbackRequest.builder()
                .patientId(1L).appointmentId(1L).rating(5).comment("Great doctor").build();

        // Act
        FeedbackResponse result = feedbackService.submit(request);

        // Assert
        assertThat(result.getRating()).isEqualTo(5);
        assertThat(result.getComment()).isEqualTo("Great doctor");
        assertThat(result.getAppointment().getAppointmentId()).isEqualTo(1L);
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void should_throwResourceNotFoundException_when_doctorNotFoundOnAverageRating() {
        // Arrange
        when(doctorRepo.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> feedbackService.getAverageRatingForDoctor(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Doctor not found");
    }

    @Test
    void should_returnAverageRating_when_doctorHasFeedback() {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));
        when(feedbackRepo.averageRatingForDoctor(1L)).thenReturn(4.5);
        when(feedbackRepo.countByAppointment_Doctor_Id(1L)).thenReturn(2L);

        // Act
        DoctorAverageRatingResponse result = feedbackService.getAverageRatingForDoctor(1L);

        // Assert
        assertThat(result.getDoctorId()).isEqualTo(1L);
        assertThat(result.getAverageRating()).isEqualTo(4.5);
        assertThat(result.getFeedbackCount()).isEqualTo(2L);
    }

    @Test
    void should_returnNullAverageRating_when_doctorHasNoFeedback() {
        // Arrange
        Doctor doctor = new Doctor();
        doctor.setId(1L);
        when(doctorRepo.findById(1L)).thenReturn(Optional.of(doctor));
        when(feedbackRepo.averageRatingForDoctor(1L)).thenReturn(null);
        when(feedbackRepo.countByAppointment_Doctor_Id(1L)).thenReturn(0L);

        // Act
        DoctorAverageRatingResponse result = feedbackService.getAverageRatingForDoctor(1L);

        // Assert
        assertThat(result.getAverageRating()).isNull();
        assertThat(result.getFeedbackCount()).isZero();
    }
}
