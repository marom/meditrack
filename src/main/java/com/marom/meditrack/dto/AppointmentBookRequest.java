package com.marom.meditrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentBookRequest {
    private Long patientId;
    private Long doctorId;
    private LocalDate scheduledDate;
}
