package com.marom.meditrack.controller;

import com.marom.meditrack.dto.PatientRequest;
import com.marom.meditrack.dto.PatientResponse;
import com.marom.meditrack.service.PatientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Patient")
@RestController
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/patients")
    public List<PatientResponse> all() {
        return patientService.findAll();
    }

    @PostMapping("/patients")
    public PatientResponse register(@RequestBody PatientRequest request) {
        return patientService.register(request);
    }
}
