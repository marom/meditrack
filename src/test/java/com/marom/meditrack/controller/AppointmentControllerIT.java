package com.marom.meditrack.controller;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppointmentControllerIT extends AbstractIntegrationTest {

    AppointmentControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    @Test
    void should_return200WithRequestedAppointment_when_bookingWithAnAvailableSlot() throws Exception {
        // Arrange
        String body = bookRequest(1L, 1L, "2026-09-01");

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId", notNullValue()))
                .andExpect(jsonPath("$.appointmentNo", matchesPattern("APT-[0-9A-F]{8}")))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.patient.id").value(1))
                .andExpect(jsonPath("$.doctor.id").value(1))
                .andExpect(jsonPath("$.scheduledDate").value("2026-09-01"))
                .andExpect(jsonPath("$.totalAmount").value(600.00))
                .andExpect(jsonPath("$.payment.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.payment.amount").value(600.00))
                .andExpect(jsonPath("$.services").isArray())
                .andExpect(jsonPath("$.services").isEmpty());
    }

    @Test
    void should_return404_when_bookingForUnknownPatient() throws Exception {
        // Arrange
        String body = bookRequest(999_999L, 1L, "2026-09-01");

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Patient not found: 999999"))
                .andExpect(jsonPath("$.path").value("/api/v1/appointments/book"));
    }

    @Test
    void should_return404_when_bookingForUnknownDoctor() throws Exception {
        // Arrange
        String body = bookRequest(1L, 999_999L, "2026-09-01");

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor not found: 999999"));
    }

    @Test
    void should_return400_when_doctorHasNoRemainingSlotsOnThatDate() throws Exception {
        // Arrange — doctor 5 has daily_slot_capacity = 5; fill the day.
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/appointments/book")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookRequest(1L, 5L, "2026-09-02")))
                    .andExpect(status().isOk());
        }

        // Act & Assert — the 6th booking is rejected.
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookRequest(1L, 5L, "2026-09-02")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", startsWith("No slots available")));
    }

    @Test
    void should_return400WithFieldError_when_bookingRequestOmitsPatientId() throws Exception {
        // Arrange — @Valid rejects the missing id before it reaches the service.
        String body = "{\"doctorId\":1,\"scheduledDate\":\"2026-09-01\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.patientId").exists());
    }

    @Test
    void should_return200WithAppointment_when_gettingExistingAppointmentById() throws Exception {
        // Arrange
        long id = book(1L, 1L, "2026-09-03");

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value((int) id))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void should_return404_when_gettingUnknownAppointmentById() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found: 999999"));
    }

    @Test
    void should_return200WithTheBookedAppointment_when_listingAppointments() throws Exception {
        // Arrange — no appointments are seeded, so the rollback leaves exactly one.
        long id = book(1L, 2L, "2026-09-04");

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].appointmentId").value((int) id));
    }

    @Test
    void should_cancelAndRefundThePayment_when_cancellingAnAppointment() throws Exception {
        // Arrange
        long id = book(1L, 1L, "2026-09-05");

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.payment.paymentStatus").value("REFUNDED"));
    }

    @Test
    void should_return404_when_cancellingUnknownAppointment() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found: 999999"));
    }

    @Test
    void should_completeAndSettleThePayment_when_completingAnAppointment() throws Exception {
        // Arrange
        long id = book(1L, 1L, "2026-09-06");

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.payment.paymentStatus").value("PAID"));
    }

    @Test
    void should_return400_when_cancellingAnAlreadyCompletedAppointment() throws Exception {
        // Arrange
        long id = book(1L, 1L, "2026-09-07");
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id)).andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", startsWith("Cannot move appointment")));
    }

    @Test
    void should_return400_when_completingAnAlreadyCancelledAppointment() throws Exception {
        // Arrange
        long id = book(1L, 1L, "2026-09-08");
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id)).andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Cannot move appointment")));
    }

    @Test
    void should_freeTheSlot_when_aBookedAppointmentIsCancelled() throws Exception {
        // Arrange — doctor 5 has daily_slot_capacity = 5; fill the day, then cancel one.
        long firstId = book(1L, 5L, "2026-09-09");
        for (int i = 0; i < 4; i++) {
            book(1L, 5L, "2026-09-09");
        }
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", firstId)).andExpect(status().isOk());

        // Act & Assert — the freed slot lets one more booking through.
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookRequest(1L, 5L, "2026-09-09")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    private String bookRequest(Long patientId, Long doctorId, String scheduledDate) throws Exception {
        return objectMapper.writeValueAsString(AppointmentBookRequest.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .scheduledDate(LocalDate.parse(scheduledDate))
                .build());
    }

    private long book(Long patientId, Long doctorId, String scheduledDate) throws Exception {
        String json = mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookRequest(patientId, doctorId, scheduledDate)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.appointmentId")).longValue();
    }
}
