package com.marom.meditrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {
    private String name;
    private String licenseNo;
    private BigDecimal consultationFee;
    private int dailySlotCapacity;
    private boolean active;
    private Long specialtyId;
}
