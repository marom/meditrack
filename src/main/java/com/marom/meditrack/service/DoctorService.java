package com.marom.meditrack.service;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.dto.DoctorResponse;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Specialty;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepo;
    private final SpecialtyRepository specialtyRepo;

    public List<DoctorResponse> findAll() {
        return doctorRepo.findAll().stream()
                .map(DoctorService::toResponse)
                .toList();
    }

    public DoctorResponse findById(Long id) {
        Doctor doctor = doctorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
        return toResponse(doctor);
    }

    public DoctorResponse create(DoctorRequest request) {
        Specialty specialty = specialtyRepo.findById(request.getSpecialtyId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialty not found: " + request.getSpecialtyId()));

        Doctor d = new Doctor();
        d.setName(request.getName());
        d.setLicenseNo(request.getLicenseNo());
        d.setConsultationFee(request.getConsultationFee());
        d.setDailySlotCapacity(request.getDailySlotCapacity());
        d.setActive(request.isActive());
        d.setSpecialty(specialty);
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return toResponse(doctorRepo.save(d));
    }

    static DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .licenseNo(d.getLicenseNo())
                .consultationFee(d.getConsultationFee())
                .dailySlotCapacity(d.getDailySlotCapacity())
                .active(d.isActive())
                .specialty(d.getSpecialty() != null ? SpecialtyService.toResponse(d.getSpecialty()) : null)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
