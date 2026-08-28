package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorAverageRatingResponse;
import com.marom.meditrack.dto.FeedbackRequest;
import com.marom.meditrack.dto.FeedbackResponse;
import com.marom.meditrack.exception.BusinessRuleException;
import com.marom.meditrack.exception.DuplicateResourceException;
import com.marom.meditrack.exception.ResourceNotFoundException;
import com.marom.meditrack.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    FeedbackControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void should_return201WithFeedbackBody_when_submitSucceeds() throws Exception {
        // Arrange
        FeedbackResponse response = FeedbackResponse.builder()
                .id(11L)
                .rating(4)
                .comment("Great care")
                .createdAt(LocalDateTime.parse("2026-08-28T10:00:00"))
                .build();
        given(feedbackService.submit(any(FeedbackRequest.class))).willReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody(4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Great care"));
        verify(feedbackService).submit(any(FeedbackRequest.class));
    }

    @Test
    void should_return400_when_submitThrowsBusinessRule() throws Exception {
        // Arrange
        given(feedbackService.submit(any(FeedbackRequest.class)))
                .willThrow(new BusinessRuleException("Feedback can only be submitted for a COMPLETED appointment"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody(4)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Feedback can only be submitted for a COMPLETED appointment"));
    }

    @Test
    void should_return400WithFieldError_when_ratingIsOutOfRange() throws Exception {
        // Arrange — rating 9 fails @Max(5); the service is never called.

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody(9)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
        verify(feedbackService, never()).submit(any());
    }

    @Test
    void should_return409_when_submitThrowsDuplicateResource() throws Exception {
        // Arrange
        given(feedbackService.submit(any(FeedbackRequest.class)))
                .willThrow(new DuplicateResourceException("Feedback already submitted for appointment 3"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody(4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Feedback already submitted for appointment 3"));
    }

    @Test
    void should_return404_when_submitThrowsResourceNotFound() throws Exception {
        // Arrange
        given(feedbackService.submit(any(FeedbackRequest.class)))
                .willThrow(new ResourceNotFoundException("Appointment not found: 999"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedbackBody(4)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found: 999"));
    }

    @Test
    void should_return200WithAverageRating_when_gettingDoctorAverage() throws Exception {
        // Arrange
        given(feedbackService.getAverageRatingForDoctor(3L)).willReturn(DoctorAverageRatingResponse.builder()
                .doctorId(3L)
                .averageRating(4.5)
                .feedbackCount(2)
                .build());

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}/average-rating", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.feedbackCount").value(2));
        verify(feedbackService).getAverageRatingForDoctor(3L);
    }

    private String feedbackBody(int rating) throws Exception {
        return objectMapper.writeValueAsString(FeedbackRequest.builder()
                .patientId(1L)
                .appointmentId(3L)
                .rating(rating)
                .comment("Great care")
                .build());
    }
}
