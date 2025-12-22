package com.ecommerce.ratingmicroservice.dto.response;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private Double averageRating;
    private Integer totalReviews;
}