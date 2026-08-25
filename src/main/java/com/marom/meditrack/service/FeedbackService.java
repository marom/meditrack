package com.marom.meditrack.service;

import com.marom.meditrack.dto.DoctorAverageRatingResponse;
import com.marom.meditrack.dto.FeedbackRequest;
import com.marom.meditrack.dto.FeedbackResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Feedback;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.repo.AppointmentRepository;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.FeedbackRepository;
import com.marom.meditrack.repo.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepo;
    private final AppointmentRepository appointmentRepo;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;

    public FeedbackResponse submit(FeedbackRequest request) {
        Appointment appointment = appointmentRepo.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + request.getAppointmentId()));

        if (!"COMPLETED".equals(appointment.getStatus())) {
            throw new BusinessRuleException("Feedback can only be submitted for a COMPLETED appointment");
        }

        Patient patient = patientRepo.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.getPatientId()));

        if (!patient.getId().equals(appointment.getPatient().getId())) {
            throw new BusinessRuleException("Patient does not match the appointment's patient");
        }

        if (feedbackRepo.existsByPatient_IdAndAppointment_AppointmentId(patient.getId(), appointment.getAppointmentId())) {
            throw new DuplicateResourceException("Feedback already submitted for appointment " + appointment.getAppointmentId());
        }

        Integer rating = request.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessRuleException("Rating must be between 1 and 5");
        }

        Feedback f = new Feedback();
        f.setPatient(patient);
        f.setAppointment(appointment);
        f.setRating(rating);
        f.setComment(request.getComment());
        f.setCreatedAt(LocalDateTime.now());
        return toResponse(feedbackRepo.save(f));
    }

    public DoctorAverageRatingResponse getAverageRatingForDoctor(Long doctorId) {
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        Double avg = feedbackRepo.averageRatingForDoctor(doctor.getId());
        long count = feedbackRepo.countByAppointment_Doctor_Id(doctor.getId());

        return DoctorAverageRatingResponse.builder()
                .doctorId(doctor.getId())
                .averageRating(avg)
                .feedbackCount(count)
                .build();
    }

    static FeedbackResponse toResponse(Feedback f) {
        return FeedbackResponse.builder()
                .id(f.getId())
                .appointment(AppointmentService.toResponse(f.getAppointment()))
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build();
    }
}
