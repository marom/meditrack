package com.marom.meditrack.service;

import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.model.Specialty;
import com.marom.meditrack.repo.SpecialtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepo;

    public List<SpecialtyResponse> findAll() {
        return specialtyRepo.findAll().stream()
                .map(SpecialtyService::toResponse)
                .toList();
    }

    public SpecialtyResponse create(SpecialtyRequest request) {
        Specialty s = new Specialty();
        s.setName(request.getName());
        s.setSlug(request.getSlug());
        s.setDescription(request.getDescription());
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return toResponse(specialtyRepo.save(s));
    }

    static SpecialtyResponse toResponse(Specialty s) {
        return SpecialtyResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .slug(s.getSlug())
                .description(s.getDescription())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
