package com.marom.meditrack.controller;

import com.marom.meditrack.dto.PatientRequest;
import com.marom.meditrack.dto.PatientResponse;
import com.marom.meditrack.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
class PatientControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private PatientService patientService;

    PatientControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return200WithAllPatients_when_listing() throws Exception {
        // Arrange
        given(patientService.findAll()).willReturn(List.of(PatientResponse.builder()
                .id(1L).firstName("John").lastName("Doe").email("john.doe@example.com").build()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    void should_return200WithRegisteredPatient_when_registering() throws Exception {
        // Arrange
        given(patientService.register(any(PatientRequest.class))).willReturn(PatientResponse.builder()
                .id(4L).firstName("Alice").lastName("Wong").email("alice.wong@example.com").build());
        String body = objectMapper.writeValueAsString(PatientRequest.builder()
                .firstName("Alice").lastName("Wong").email("alice.wong@example.com")
                .phone("+1-202-555-0000").dateOfBirth(LocalDate.parse("1995-06-15")).build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.email").value("alice.wong@example.com"));
        verify(patientService).register(any(PatientRequest.class));
    }

    @Test
    void should_return500_when_serviceThrowsUnexpectedError() throws Exception {
        // Arrange — e.g. a DB unique-key violation bubbling up as a runtime exception.
        given(patientService.register(any(PatientRequest.class)))
                .willThrow(new RuntimeException("could not execute statement"));
        String body = objectMapper.writeValueAsString(PatientRequest.builder()
                .firstName("Johnny").lastName("Doppelganger").email("john.doe@example.com").build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/patients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
