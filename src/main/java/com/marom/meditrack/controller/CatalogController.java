package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.dto.DoctorResponse;
import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.service.DoctorService;
import com.marom.meditrack.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SMELL: one controller for two resources, no /api/v1 prefix, no validation,
//        no error handling.
@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final SpecialtyService specialtyService;
    private final DoctorService doctorService;

    @Operation(tags = "Specialty")
    @GetMapping("/specialties")
    public List<SpecialtyResponse> allSpecialties() {
        return specialtyService.findAll();
    }

    @Operation(tags = "Specialty")
    @PostMapping("/specialties")
    public SpecialtyResponse createSpecialty(@RequestBody SpecialtyRequest request) {
        return specialtyService.create(request);
    }

    @Operation(tags = "Doctor")
    @GetMapping("/doctors")
    public List<DoctorResponse> allDoctors() {
        return doctorService.findAll();
    }

    @Operation(tags = "Doctor")
    @GetMapping("/doctors/{id}")
    public DoctorResponse getDoctor(@PathVariable Long id) {
        return doctorService.findById(id);   // SMELL: returns null on miss
    }

    @Operation(tags = "Doctor")
    @PostMapping("/doctors")
    public DoctorResponse createDoctor(@RequestBody DoctorRequest request) {
        return doctorService.create(request);
    }
}
