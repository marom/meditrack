package com.marom.meditrack.controller;

import com.marom.meditrack.model.Appointment;
import com.marom.meditrack.repo.AppointmentRepository;
import com.marom.meditrack.service.AppointmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SMELL: still no DTOs, no exception handling, status as free text with no
//        transition rules, cancel does not restore the slot or refund the payment.
//        Booking/cancel business logic now lives in AppointmentService.
@Tag(name = "Appointment Management")
@RestController
public class AppointmentController {

    @Autowired
    private AppointmentRepository appointmentRepo;
    @Autowired
    private AppointmentService appointmentService;

    @GetMapping("/appointments")
    public List<Appointment> all() {
        return appointmentRepo.findAll();
    }

    @GetMapping("/appointments/{id}")
    public Appointment get(@PathVariable Long id) {
        return appointmentRepo.findById(id).orElse(null);
    }

    @PostMapping("/appointments/book")
    public Appointment book(@RequestParam Long patientId,
                            @RequestParam Long doctorId,
                            @RequestParam String date) {
        return appointmentService.book(patientId, doctorId, date);
    }

    @PostMapping("/appointments/{id}/cancel")
    public Appointment cancel(@PathVariable Long id) {
        return appointmentService.cancel(id);
    }
}
