package com.playstop.backend.dto.response;

import com.playstop.backend.enums.ReservationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReservationResponse {
    private UUID id;
    private String courtName;
    private String sportType;
    private String courtAddress;
    private LocalDate date;
    private int slotHour;
    private String slotLabel;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    // Client info — visible to the court owner
    private String clientName;
    private String clientEmail;
}