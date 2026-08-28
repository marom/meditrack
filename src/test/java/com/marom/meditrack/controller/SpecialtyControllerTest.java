package com.marom.meditrack.controller;

import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.dto.SpecialtyResponse;
import com.marom.meditrack.service.SpecialtyService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

@WebMvcTest(SpecialtyController.class)
class SpecialtyControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private SpecialtyService specialtyService;

    SpecialtyControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return200WithAllSpecialties_when_listing() throws Exception {
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
    void should_return200WithCreatedSpecialty_when_creating() throws Exception {
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
    void should_return400_when_creatingWithBlankSlug() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(SpecialtyRequest.builder()
                .name("Neurology").slug("").description("Brain").build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.slug").exists());
        verify(specialtyService, never()).create(any());
    }
}
