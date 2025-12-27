package com.ecommerce.ratingmicroservice.controller;

import com.ecommerce.ratingmicroservice.dto.request.ReviewRequest;
import com.ecommerce.ratingmicroservice.dto.response.PageResponse;
import com.ecommerce.ratingmicroservice.dto.response.ReviewResponse;
import com.ecommerce.ratingmicroservice.entity.Review;
import com.ecommerce.ratingmicroservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API endpoints for review management.
 * Uses PageResponse for cached pagination support.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Create a new review for a product
     * Requires authentication
     *
     * @param request Review data including productId, rating, and comment
     * @return Created review with status PENDING
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing review
     * Only the review owner can update their review
     *
     * @param id Review ID to update
     * @param request Updated review data
     * @return Updated review
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a review
     * Only the review owner or ADMIN can delete
     *
     * @param id Review ID to delete
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a single review by ID
     * Public endpoint - returns review if it exists
     *
     * @param id Review ID
     * @return Review details
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable String id) {
        ReviewResponse response = reviewService.getReviewById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get paginated reviews for a specific product
     * Only shows APPROVED reviews to public
     * Results are cached for better performance
     *
     * @param productId Product ID
     * @param pageable Pagination parameters (page, size, sort)
     * @return Paginated list of approved reviews
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviewsByProduct(
            @PathVariable String productId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        PageResponse<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get all reviews by a specific user
     * User can see all their own reviews (any status)
     * Others can only see APPROVED reviews
     * ADMIN can see any user's reviews
     *
     * @param userId User ID
     * @return List of reviews
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getReviewsByUser(@PathVariable String userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Moderate a review (approve or reject)
     * Only MODERATOR or ADMIN can access this endpoint
     * Only PENDING reviews can be moderated
     *
     * Example usage:
     * PATCH /api/reviews/abc123/moderate?status=APPROVED
     * PATCH /api/reviews/abc123/moderate?status=REJECTED
     *
     * @param id Review ID to moderate
     * @param status New status (APPROVED or REJECTED)
     * @return Moderated review
     */
    @PatchMapping("/{id}/moderate")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public ResponseEntity<ReviewResponse> moderateReview(
            @PathVariable String id,
            @RequestParam Review.Status status) {
        ReviewResponse response = reviewService.moderateReview(id, status);
        return ResponseEntity.ok(response);
    }
}

// The @PreAuthorize uses SpEL (Spring Expression Language):
// #userId binds to the @PathVariable
// authentication.principal.id accesses UserPrincipal.getId()
// hasRole('ADMIN') checks if user has ADMIN role
// hasAnyRole('MODERATOR', 'ADMIN') checks if user has either role