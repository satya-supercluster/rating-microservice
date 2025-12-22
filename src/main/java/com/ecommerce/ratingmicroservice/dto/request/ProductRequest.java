package com.ecommerce.ratingmicroservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String category;

    @NotNull
    @Positive
    private BigDecimal price;

    private String imageUrl;
}