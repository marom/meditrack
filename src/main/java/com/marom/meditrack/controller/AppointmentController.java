package com.marom.meditrack.controller;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
import com.marom.meditrack.service.AppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SMELL: status as free text with no transition rules,
//        cancel does not restore the slot or refund the payment.
@Tag(name = "Appointment Management")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public List<AppointmentResponse> all() {
        return appointmentService.findAll();
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id) {
        return appointmentService.findById(id);
    }

    @PostMapping("/book")
    public AppointmentResponse book(@RequestBody AppointmentBookRequest request) {
        return appointmentService.book(request);
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return appointmentService.complete(id);
    }
}
