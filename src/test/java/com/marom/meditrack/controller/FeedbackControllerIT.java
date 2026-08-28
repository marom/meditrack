package com.marom.meditrack.controller;

import com.marom.meditrack.dto.AppointmentBookRequest;
import com.marom.meditrack.dto.FeedbackRequest;
import com.marom.meditrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FeedbackControllerIT extends AbstractIntegrationTest {

    FeedbackControllerIT(MockMvc mockMvc, ObjectMapper objectMapper) {
        super(mockMvc, objectMapper);
    }

    @Test
    void should_return400_when_submittingFeedbackForANonCompletedAppointment() throws Exception {
        // Arrange
        long appointmentId = book(1L, 1L, "2026-09-10");

        // Act & Assert
        submitFeedback(1L, appointmentId, 5, "Nice")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Feedback can only be submitted for a COMPLETED appointment"));
    }

    @Test
    void should_return201WithFeedback_when_submittingForACompletedAppointment() throws Exception {
        // Arrange
        long appointmentId = completedAppointment(1L, 1L, "2026-09-11");

        // Act & Assert
        submitFeedback(1L, appointmentId, 4, "Great care")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Great care"))
                .andExpect(jsonPath("$.appointment.appointmentId").value((int) appointmentId))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void should_return409_when_submittingFeedbackTwiceForTheSameAppointment() throws Exception {
        // Arrange
        long appointmentId = completedAppointment(1L, 1L, "2026-09-12");
        submitFeedback(1L, appointmentId, 5, "First").andExpect(status().isCreated());

        // Act & Assert
        submitFeedback(1L, appointmentId, 3, "Second")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", startsWith("Feedback already submitted")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void should_return400_when_ratingIsOutOfRange(int rating) throws Exception {
        // Arrange
        long appointmentId = completedAppointment(1L, 1L, "2026-09-13");

        // Act & Assert
        submitFeedback(1L, appointmentId, rating, "x")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must be between 1 and 5"));
    }

    @Test
    void should_return400_when_ratingIsMissing() throws Exception {
        // Arrange
        long appointmentId = completedAppointment(1L, 1L, "2026-09-14");

        // Act & Assert
        submitFeedback(1L, appointmentId, null, "no rating")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating must be between 1 and 5"));
    }

    @Test
    void should_return400_when_theSubmittingPatientDoesNotMatchTheAppointment() throws Exception {
        // Arrange — appointment belongs to patient 1, feedback claims patient 2.
        long appointmentId = completedAppointment(1L, 1L, "2026-09-15");

        // Act & Assert
        submitFeedback(2L, appointmentId, 5, "mismatch")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patient does not match the appointment's patient"));
    }

    @Test
    void should_return404_when_submittingFeedbackForUnknownAppointment() throws Exception {
        // Act & Assert
        submitFeedback(1L, 999_999L, 5, "ghost")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Appointment not found: 999999"));
    }

    @Test
    void should_returnNullAverageAndZeroCount_when_doctorHasNoFeedback() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}/average-rating", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(2))
                .andExpect(jsonPath("$.averageRating", nullValue()))
                .andExpect(jsonPath("$.feedbackCount").value(0));
    }

    @Test
    void should_returnMeanRatingAndCount_when_doctorHasFeedback() throws Exception {
        // Arrange — doctor 3 gets ratings 4 and 2 from two different patients.
        long first = completedAppointment(1L, 3L, "2026-09-16");
        submitFeedback(1L, first, 4, "good").andExpect(status().isCreated());
        long second = completedAppointment(2L, 3L, "2026-09-17");
        submitFeedback(2L, second, 2, "meh").andExpect(status().isCreated());

        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}/average-rating", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(3))
                .andExpect(jsonPath("$.averageRating").value(3.0))
                .andExpect(jsonPath("$.feedbackCount").value(2));
    }

    @Test
    void should_return404_when_gettingAverageRatingForUnknownDoctor() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/doctors/{id}/average-rating", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Doctor not found: 999999"));
    }

    private ResultActions submitFeedback(Long patientId, Long appointmentId, Integer rating, String comment)
            throws Exception {
        String body = objectMapper.writeValueAsString(FeedbackRequest.builder()
                .patientId(patientId)
                .appointmentId(appointmentId)
                .rating(rating)
                .comment(comment)
                .build());
        return mockMvc.perform(post("/api/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long book(Long patientId, Long doctorId, String scheduledDate) throws Exception {
        String body = objectMapper.writeValueAsString(AppointmentBookRequest.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .scheduledDate(LocalDate.parse(scheduledDate))
                .build());
        String json = mockMvc.perform(post("/api/v1/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.appointmentId")).longValue();
    }

    private long completedAppointment(Long patientId, Long doctorId, String scheduledDate) throws Exception {
        long id = book(patientId, doctorId, scheduledDate);
        mockMvc.perform(post("/api/v1/appointments/{id}/complete", id))
                .andExpect(status().isOk());
        return id;
    }
}
