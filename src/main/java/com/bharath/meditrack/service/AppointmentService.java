package com.bharath.meditrack.service;

import com.bharath.meditrack.exception.ResourceNotFoundException;
import com.bharath.meditrack.model.Appointment;
import com.bharath.meditrack.model.Doctor;
import com.bharath.meditrack.model.Patient;
import com.bharath.meditrack.repo.AppointmentRepository;
import com.bharath.meditrack.repo.DoctorRepository;
import com.bharath.meditrack.repo.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// SMELL (still present, out of scope for this extraction): status as free
// text, slot-capacity failure as generic RuntimeException, cancel does not
// release the doctor's slot or refund the payment.
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepo;
    private final DoctorRepository doctorRepo;
    private final PatientRepository patientRepo;

    public Appointment book(Long patientId, Long doctorId, String date) {
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));

        LocalDate scheduled = LocalDate.parse(date);
        long booked = appointmentRepo.countByDoctorAndScheduledDate(doctor, scheduled);
        if (booked >= doctor.getDailySlotCapacity()) {
            throw new RuntimeException("No slots available for this doctor on " + date);
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
        return appointmentRepo.save(a);
    }

    public Appointment cancel(Long id) {
        Appointment a = appointmentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        a.setStatus("CANCELLED");
        a.setUpdatedAt(LocalDateTime.now());
        return appointmentRepo.save(a);
    }
}
