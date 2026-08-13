package com.bharath.meditrack.controller;

import com.bharath.meditrack.model.Appointment;
import com.bharath.meditrack.repo.AppointmentRepository;
import com.bharath.meditrack.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// SMELL: still no DTOs, no exception handling, status as free text with no
//        transition rules, cancel does not restore the slot or refund the payment.
//        Booking/cancel business logic now lives in AppointmentService.
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
