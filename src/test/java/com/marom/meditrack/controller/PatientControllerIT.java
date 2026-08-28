package com.marom.meditrack.controller;

import com.marom.meditrack.dto.PatientRequest;
import com.marom.meditrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientControllerIT extends AbstractIntegrationTest {

    PatientControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    @Test
    void should_return200WithAllSeededPatients_when_listingPatients() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    void should_return200WithRegisteredPatient_when_registeringANewPatient() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(PatientRequest.builder()
                .firstName("Alice")
                .lastName("Wong")
                .email("alice.wong@example.com")
                .phone("+1-202-555-0000")
                .dateOfBirth(LocalDate.parse("1995-06-15"))
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice.wong@example.com"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void should_return409_when_registeringWithAnAlreadyUsedEmail() throws Exception {
        // Arrange — email john.doe@example.com is seeded.
        String body = objectMapper.writeValueAsString(PatientRequest.builder()
                .firstName("Johnny")
                .lastName("Doppelganger")
                .email("john.doe@example.com")
                .phone("+1-202-555-9999")
                .dateOfBirth(LocalDate.parse("1990-05-14"))
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", startsWith("Patient already exists with email")));
    }

    @Test
    void should_return400WithFieldErrors_when_registeringWithInvalidPayload() throws Exception {
        // Arrange — blank last name, malformed email.
        String body = objectMapper.writeValueAsString(PatientRequest.builder()
                .firstName("Alice")
                .lastName("")
                .email("nope")
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }
}
