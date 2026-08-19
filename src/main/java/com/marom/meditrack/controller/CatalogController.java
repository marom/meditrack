package com.marom.meditrack.controller;

import com.marom.meditrack.model.Doctor;
import com.marom.meditrack.model.Specialty;
import com.marom.meditrack.repo.DoctorRepository;
import com.marom.meditrack.repo.SpecialtyRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// SMELL: one controller for two resources, field injection, no /api/v1 prefix,
//        returns entities directly (no DTOs), no validation, no error handling.
@RestController
public class CatalogController {

    @Autowired
    private SpecialtyRepository specialtyRepo;
    @Autowired
    private DoctorRepository doctorRepo;

    @Operation(tags = "Specialty")
    @GetMapping("/specialties")
    public List<Specialty> allSpecialties() {
        return specialtyRepo.findAll();
    }

    @Operation(tags = "Specialty")
    @PostMapping("/specialties")
    public Specialty createSpecialty(@RequestBody Specialty s) {
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return specialtyRepo.save(s);
    }

    @Operation(tags = "Doctor")
    @GetMapping("/doctors")
    public List<Doctor> allDoctors() {
        return doctorRepo.findAll();
    }

    @Operation(tags = "Doctor")
    @GetMapping("/doctors/{id}")
    public Doctor getDoctor(@PathVariable Long id) {
        return doctorRepo.findById(id).orElse(null);   // SMELL: returns null on miss
    }

    @Operation(tags = "Doctor")
    @PostMapping("/doctors")
    public Doctor createDoctor(@RequestBody Doctor d) {
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return doctorRepo.save(d);
    }
}
