package com.marom.meditrack.controller;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.AppointmentResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.model.AppointmentStatus;
import com.marom.meditrack.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private AppointmentService appointmentService;

    AppointmentControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return200WithAppointmentBody_when_bookingSucceeds() throws Exception {
        // Arrange
        given(appointmentService.book(any(AppointmentBookRequest.class)))
                .willReturn(sampleAppointment(AppointmentStatus.REQUESTED));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(42))
                .andExpect(jsonPath("$.appointmentNo").value("APT-1A2B3C4D"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.totalAmount").value(600.00));
        verify(appointmentService).book(any(AppointmentBookRequest.class));
    }

    @Test
    void should_return404_when_serviceThrowsResourceNotFound() throws Exception {
        // Arrange
        given(appointmentService.book(any(AppointmentBookRequest.class)))
                .willThrow(new ResourceNotFoundException("Patient not found: 5"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Patient not found: 5"))
                .andExpect(jsonPath("$.path").value("/api/v1/appointments/book"));
    }

    @Test
    void should_return400_when_serviceThrowsBusinessRule() throws Exception {
        // Arrange
        given(appointmentService.book(any(AppointmentBookRequest.class)))
                .willThrow(new BusinessRuleException("No slots available for this doctor on 2026-09-01"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("No slots available for this doctor on 2026-09-01"));
    }

    @Test
    void should_return400_when_bookingRequestMissingPatientId() throws Exception {
        // Arrange — patientId omitted; @Valid rejects it before the service runs.
        String body = objectMapper.writeValueAsString(AppointmentBookRequest.builder()
                .doctorId(1L)
                .scheduledDate(LocalDate.parse("2026-09-01"))
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.patientId").exists());
        verify(appointmentService, never()).book(any());
    }

    @Test
    void should_return200WithCancelledAppointment_when_cancelling() throws Exception {
        // Arrange
        given(appointmentService.cancel(7L)).willReturn(sampleAppointment(AppointmentStatus.CANCELLED));

        // Act & Assert
        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(appointmentService).cancel(7L);
    }

    @Test
    void should_return200WithAllAppointments_when_listing() throws Exception {
        // Arrange
        given(appointmentService.findAll()).willReturn(List.of(sampleAppointment(AppointmentStatus.REQUESTED)));

        // Act & Assert
        mockMvc.perform(get("/api/v1/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].appointmentId").value(42));
    }

    private String bookBody() throws Exception {
        return objectMapper.writeValueAsString(AppointmentBookRequest.builder()
                .patientId(1L)
                .doctorId(1L)
                .scheduledDate(LocalDate.parse("2026-09-01"))
                .build());
    }

    private AppointmentResponse sampleAppointment(AppointmentStatus status) {
        return AppointmentResponse.builder()
                .appointmentId(42L)
                .appointmentNo("APT-1A2B3C4D")
                .status(status)
                .scheduledDate(LocalDate.parse("2026-09-01"))
                .totalAmount(new BigDecimal("600.00"))
                .services(List.of())
                .build();
    }
}
