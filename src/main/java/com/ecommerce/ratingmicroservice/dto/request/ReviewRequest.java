package com.ecommerce.ratingmicroservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private String productId;

    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}