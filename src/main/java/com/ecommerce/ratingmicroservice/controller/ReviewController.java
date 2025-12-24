 package com.ecommerce.ratingmicroservice.controller;

import com.ecommerce.ratingmicroservice.dto.request.ReviewRequest;
import com.ecommerce.ratingmicroservice.dto.response.ReviewResponse;
import com.ecommerce.ratingmicroservice.entity.Review;
import com.ecommerce.ratingmicroservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByProduct(
            @PathVariable String productId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        Page<ReviewResponse> reviews = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getReviewsByUser(@PathVariable String userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByUser(userId);
        return ResponseEntity.ok(reviews);
    }

    // PATCH /api/reviews/abc123/moderate?status=APPROVED
    // PATCH /api/reviews/abc123/moderate?status=REJECTED
    // With Appropriate Token
    @PatchMapping("/{id}/moderate")
    public ResponseEntity<ReviewResponse> moderateReview(
            @PathVariable String id,
            @RequestParam Review.Status status) {
        ReviewResponse response = reviewService.moderateReview(id, status);
        return ResponseEntity.ok(response);
    }
}


//The @PreAuthorize uses SpEL:
//#userId binds to path variable
//authentication.principal.id accesses UserPrincipal.getId()