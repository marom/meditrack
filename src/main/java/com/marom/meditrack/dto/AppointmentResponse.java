package com.marom.meditrack.dto;

import com.marom.meditrack.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long appointmentId;
    private String appointmentNo;
    private PatientResponse patient;
    private DoctorResponse doctor;
    private AppointmentStatus status;
    private LocalDate scheduledDate;
    private BigDecimal totalAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PaymentResponse payment;
    private List<AppointmentServiceResponse> services;
}
