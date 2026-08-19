package com.marom.meditrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorResponse {
    private Long id;
    private String name;
    private String licenseNo;
    private BigDecimal consultationFee;
    private int dailySlotCapacity;
    private boolean active;
    private SpecialtyResponse specialty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
