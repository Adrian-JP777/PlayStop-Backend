package com.playstop.backend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El tipo de deporte es obligatorio")
    private String sportType;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.1", message = "El precio debe ser mayor a 0")
    private BigDecimal pricePerHour;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    private Double latitude;
    private Double longitude;
    private String imageUrl;
    private String city;
    private String district;
}