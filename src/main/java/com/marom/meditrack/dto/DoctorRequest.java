package com.marom.meditrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotBlank
    private String name;

    @NotBlank
    private String licenseNo;

    @NotNull
    @Positive
    private BigDecimal consultationFee;

    @PositiveOrZero
    private int dailySlotCapacity;

    private boolean active;

    @NotNull
    private Long specialtyId;
}
