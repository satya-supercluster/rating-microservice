package com.ecommerce.ratingmicroservice.mapper;

import com.ecommerce.ratingmicroservice.dto.request.ReviewRequest;
import com.ecommerce.ratingmicroservice.dto.response.ReviewResponse;
import com.ecommerce.ratingmicroservice.entity.Review;
import com.ecommerce.ratingmicroservice.entity.User;
import com.ecommerce.ratingmicroservice.entity.Product;

import java.time.LocalDateTime;

public class ReviewMapperUtil {

    public static Review toEntity(ReviewRequest request, User user, Product product) {
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        review.setStatus(Review.Status.PENDING);
        return review;
    }

    public static ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProduct().getId());
        response.setUserId(review.getUser().getId());
        response.setUsername(review.getUser().getUsername());
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        response.setStatus(review.getStatus());
        return response;
    }
}