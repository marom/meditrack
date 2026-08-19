package com.marom.meditrack.service;

import com.marom.meditrack.dto.PatientRequest;
import com.marom.meditrack.dto.PatientResponse;
import com.marom.meditrack.model.Patient;
import com.marom.meditrack.repo.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepo;

    public List<PatientResponse> findAll() {
        return patientRepo.findAll().stream()
                .map(PatientService::toResponse)
                .toList();
    }

    // SMELL: no duplicate-email check, no validation (preserved from the pre-DTO controller).
    public PatientResponse register(PatientRequest request) {
        Patient p = new Patient();
        p.setFirstName(request.getFirstName());
        p.setLastName(request.getLastName());
        p.setEmail(request.getEmail());
        p.setPhone(request.getPhone());
        p.setDateOfBirth(request.getDateOfBirth());
        p.setCreatedAt(LocalDateTime.now());
        return toResponse(patientRepo.save(p));
    }

    static PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .phone(p.getPhone())
                .dateOfBirth(p.getDateOfBirth())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
