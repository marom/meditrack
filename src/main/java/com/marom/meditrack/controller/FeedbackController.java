package com.marom.meditrack.controller;

import com.marom.meditrack.dto.DoctorAverageRatingResponse;
import com.marom.meditrack.dto.FeedbackRequest;
import com.marom.meditrack.dto.FeedbackResponse;
import com.marom.meditrack.service.FeedbackService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Feedback")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/feedback")
    public ResponseEntity<FeedbackResponse> submit(@Valid @RequestBody FeedbackRequest request) {
        FeedbackResponse response = feedbackService.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/doctors/{doctorId}/average-rating")
    public DoctorAverageRatingResponse averageRating(@PathVariable Long doctorId) {
        return feedbackService.getAverageRatingForDoctor(doctorId);
    }
}
