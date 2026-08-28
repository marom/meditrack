package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorRequest;
import com.marom.meditrack.dto.DoctorResponse;
import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.service.DoctorService;
import com.marom.meditrack.service.SpecialtyService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private SpecialtyService specialtyService;
    @MockitoBean
    private DoctorService doctorService;

    CatalogControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return200WithAllSpecialties_when_listingSpecialties() throws Exception {
        // Arrange
        given(specialtyService.findAll()).willReturn(List.of(
                SpecialtyResponse.builder().id(1L).name("Cardiology").slug("cardiology").build()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cardiology"));
    }

    @Test
    void should_return200WithCreatedSpecialty_when_creatingSpecialty() throws Exception {
        // Arrange
        given(specialtyService.create(any(SpecialtyRequest.class))).willReturn(
                SpecialtyResponse.builder().id(9L).name("Neurology").slug("neurology").build());
        String body = objectMapper.writeValueAsString(SpecialtyRequest.builder()
                .name("Neurology").slug("neurology").description("Brain").build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Neurology"));
        verify(specialtyService).create(any(SpecialtyRequest.class));
    }

    @Test
    void should_return200WithAllDoctors_when_listingDoctors() throws Exception {
        // Arrange
        given(doctorService.findAll()).willReturn(List.of(sampleDoctor()));

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Dr. Asha Rao"));
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
    void should_return200WithCreatedDoctor_when_creatingDoctor() throws Exception {
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
    void should_return404_when_creatingDoctorForUnknownSpecialty() throws Exception {
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
