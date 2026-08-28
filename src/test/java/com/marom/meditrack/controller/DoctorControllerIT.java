package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DoctorControllerIT extends AbstractIntegrationTest {

    DoctorControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    @Test
    void should_return200WithAllSeededDoctors_when_listing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Dr. Asha Rao"))
                .andExpect(jsonPath("$[0].consultationFee").value(600.00))
                .andExpect(jsonPath("$[0].specialty.name").value("Cardiology"));
    }

    @Test
    void should_return200WithDoctor_when_gettingExistingDoctorById() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dr. Asha Rao"))
                .andExpect(jsonPath("$.licenseNo").value("LIC-CARD-001"))
                .andExpect(jsonPath("$.specialty.id").value(1));
    }

    @Test
    void should_return404_when_gettingUnknownDoctorById() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor not found: 999999"));
    }

    @Test
    void should_return200WithCreatedDoctor_when_creatingForAnExistingSpecialty() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("Dr. New Hire")
                .licenseNo("LIC-DERM-777")
                .consultationFee(new BigDecimal("250.00"))
                .dailySlotCapacity(4)
                .active(true)
                .specialtyId(2L)
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Dr. New Hire"))
                .andExpect(jsonPath("$.consultationFee").value(250.00))
                .andExpect(jsonPath("$.dailySlotCapacity").value(4))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.specialty.id").value(2))
                .andExpect(jsonPath("$.specialty.name").value("Dermatology"));
    }

    @Test
    void should_return409_when_creatingWithASeededLicenseNumber() throws Exception {
        // Arrange — LIC-CARD-001 is already seeded for Dr. Asha Rao.
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("Dr. Clone")
                .licenseNo("LIC-CARD-001")
                .consultationFee(new BigDecimal("300.00"))
                .dailySlotCapacity(3)
                .active(true)
                .specialtyId(1L)
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_return400WithFieldErrors_when_creatingWithInvalidPayload() throws Exception {
        // Arrange — blank name, non-positive fee, missing specialty id.
        String body = "{\"name\":\"  \",\"licenseNo\":\"LIC-Z\",\"consultationFee\":0,\"dailySlotCapacity\":2,\"active\":true}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.consultationFee").exists())
                .andExpect(jsonPath("$.fieldErrors.specialtyId").exists());
    }

    @Test
    void should_return404_when_creatingForUnknownSpecialty() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("Dr. Nowhere")
                .licenseNo("LIC-NON-000")
                .consultationFee(new BigDecimal("100.00"))
                .dailySlotCapacity(1)
                .active(true)
                .specialtyId(999_999L)
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Specialty not found: 999999"));
    }
}
