package com.marom.meditrack.controller;

import com.marom.meditrack.dto.SpecialtyRequest;
import com.marom.meditrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpecialtyControllerIT extends AbstractIntegrationTest {

    SpecialtyControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    @Test
    void should_return200WithAllSeededSpecialties_when_listing() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].name").value("Cardiology"))
                .andExpect(jsonPath("$[0].slug").value("cardiology"));
    }

    @Test
    void should_return200WithCreatedSpecialty_when_creating() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(SpecialtyRequest.builder()
                .name("Neurology")
                .slug("neurology")
                .description("Brain and nervous system")
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Neurology"))
                .andExpect(jsonPath("$.slug").value("neurology"))
                .andExpect(jsonPath("$.description").value("Brain and nervous system"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void should_return409_when_creatingWithASeededName() throws Exception {
        // Arrange — "Cardiology" / "cardiology" is seeded.
        String body = objectMapper.writeValueAsString(SpecialtyRequest.builder()
                .name("Cardiology")
                .slug("cardiology")
                .description("dup")
                .build());

        // Act & Assert
        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
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
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.slug").exists());
    }
}
