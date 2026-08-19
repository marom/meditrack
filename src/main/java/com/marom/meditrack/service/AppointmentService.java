package com.marom.meditrack.service;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
import com.marom.meditrack.dto.AppointmentServiceResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.repo.AppointmentRepository;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// SMELL (still present, out of scope for this extraction): status as free
// text, cancel does not release the doctor's slot or refund the payment.
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

    public List<AppointmentResponse> findAll() {
        return appointmentRepo.findAll().stream()
                .map(AppointmentService::toResponse)
                .toList();
    }

    public AppointmentResponse findById(Long id) {
        Appointment a = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        return toResponse(a);
    }

    public AppointmentResponse book(AppointmentBookRequest request) {
        Patient patient = patientRepo.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.getPatientId()));
        Doctor doctor = doctorRepo.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.getDoctorId()));

        LocalDate scheduled = request.getScheduledDate();
        long booked = appointmentRepo.countByDoctorAndScheduledDate(doctor, scheduled);
        if (booked >= doctor.getDailySlotCapacity()) {
            throw new BusinessRuleException("No slots available for this doctor on " + scheduled);
        }

        Appointment a = new Appointment();
        a.setAppointmentNo("APT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        a.setPatient(patient);
        a.setDoctor(doctor);
        a.setStatus("REQUESTED");
        a.setScheduledDate(scheduled);
        a.setTotalAmount(doctor.getConsultationFee());
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return toResponse(appointmentRepo.save(a));
    }

    public AppointmentResponse cancel(Long id) {
        Appointment a = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        a.setStatus("CANCELLED");
        a.setUpdatedAt(LocalDateTime.now());
        return toResponse(appointmentRepo.save(a));
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
}
