package com.ecommerce.ratingmicroservice.dto.response;


import com.ecommerce.ratingmicroservice.entity.Review;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private String id;
    private String productId;
    private String userId;
    private String username; // denormalized for easier display
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Review.Status status;
}