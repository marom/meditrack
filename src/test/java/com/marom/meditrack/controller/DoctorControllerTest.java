package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.dto.DoctorResponse;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.service.DoctorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(DoctorController.class)
class DoctorControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private DoctorService doctorService;

    DoctorControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return200WithAllDoctors_when_listing() throws Exception {
        // Arrange
        given(doctorService.findAll()).willReturn(List.of(sampleDoctor()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Dr. Asha Rao"));
    }

    @Test
    void should_return200WithDoctor_when_gettingExistingDoctorById() throws Exception {
        // Arrange
        given(doctorService.findById(1L)).willReturn(sampleDoctor());

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Dr. Asha Rao"));
    }

    @Test
    void should_return404_when_gettingUnknownDoctorById() throws Exception {
        // Arrange
        given(doctorService.findById(999L)).willThrow(new ResourceNotFoundException("Doctor not found: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor not found: 999"));
    }

    @Test
    void should_return200WithCreatedDoctor_when_creating() throws Exception {
        // Arrange
        given(doctorService.create(any(DoctorRequest.class))).willReturn(sampleDoctor());
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("Dr. Asha Rao").licenseNo("LIC-CARD-001")
                .consultationFee(new BigDecimal("600.00")).dailySlotCapacity(8)
                .active(true).specialtyId(1L).build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Dr. Asha Rao"))
                .andExpect(jsonPath("$.consultationFee").value(600.00));
        verify(doctorService).create(any(DoctorRequest.class));
    }

    @Test
    void should_return404_when_creatingForUnknownSpecialty() throws Exception {
        // Arrange
        given(doctorService.create(any(DoctorRequest.class)))
                .willThrow(new ResourceNotFoundException("Specialty not found: 999"));
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("Dr. Nobody").licenseNo("LIC-X").consultationFee(new BigDecimal("1.00"))
                .dailySlotCapacity(1).active(true).specialtyId(999L).build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Specialty not found: 999"));
    }

    @Test
    void should_return400WithFieldErrors_when_creatingWithBlankNameAndNonPositiveFee() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(DoctorRequest.builder()
                .name("  ")
                .licenseNo("LIC-X")
                .consultationFee(new BigDecimal("-1.00"))
                .dailySlotCapacity(1)
                .active(true)
                .specialtyId(1L)
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/doctors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.consultationFee").exists());
        verify(doctorService, never()).create(any());
    }

    private DoctorResponse sampleDoctor() {
        return DoctorResponse.builder()
                .id(1L)
                .name("Dr. Asha Rao")
                .licenseNo("LIC-CARD-001")
                .consultationFee(new BigDecimal("600.00"))
                .dailySlotCapacity(8)
                .active(true)
                .specialty(SpecialtyResponse.builder().id(1L).name("Cardiology").slug("cardiology").build())
                .build();
    }
}
