package com.marom.meditrack.controller;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
import com.marom.meditrack.service.AppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SMELL: no /api/v1 prefix, status as free text with no transition rules,
//        cancel does not restore the slot or refund the payment.
@Tag(name = "Appointment Management")
@RestController
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/appointments")
    public List<AppointmentResponse> all() {
        return appointmentService.findAll();
    }

    @GetMapping("/appointments/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.findById(id);
    }

    @PostMapping("/appointments/book")
    public AppointmentResponse book(@RequestBody AppointmentBookRequest request) {
        return appointmentService.book(request);
    }

    @PostMapping("/appointments/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }

    // New endpoint only — no class-level @RequestMapping added, to avoid
    // prefixing the pre-existing /appointments endpoints above (out of scope).
    @PostMapping("/api/v1/appointments/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return appointmentService.complete(id);
    }
}
