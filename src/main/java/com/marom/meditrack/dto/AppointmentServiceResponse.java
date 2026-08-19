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
public class AppointmentServiceResponse {
    private Long id;
    private String serviceName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
