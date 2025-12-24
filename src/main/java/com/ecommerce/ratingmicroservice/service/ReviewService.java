package com.ecommerce.ratingmicroservice.service;

import com.ecommerce.ratingmicroservice.dto.request.ReviewRequest;
import com.ecommerce.ratingmicroservice.dto.response.ReviewResponse;
import com.ecommerce.ratingmicroservice.entity.Product;
import com.ecommerce.ratingmicroservice.entity.Review;
import com.ecommerce.ratingmicroservice.entity.User;
import com.ecommerce.ratingmicroservice.mapper.ReviewMapperUtil;
import com.ecommerce.ratingmicroservice.repository.ProductRepository;
import com.ecommerce.ratingmicroservice.repository.ReviewRepository;
import com.ecommerce.ratingmicroservice.repository.UserRepository;
import com.ecommerce.ratingmicroservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository; // You need this to update avg rating
    private final UserRepository userRepository;

    // Helper: Get current authenticated user ID
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) auth.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    // Helper: Fetch and validate user
    private User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    // Helper: Fetch and validate product
    private Product getProductById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
    }

    // --- CREATE ---
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        String currentUserId = getCurrentUserId();

        // Validate: no duplicate review
        if (reviewRepository.existsByProduct_IdAndUser_Id(request.getProductId(), currentUserId)) {
            throw new RuntimeException("You have already reviewed this product.");
        }

        // Fetch entities
        Product product = getProductById(request.getProductId());
        User user = getUserById(currentUserId);

        // Create review (default: PENDING; could auto-approve if desired)
        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setStatus(Review.Status.PENDING); // or APPROVED if no moderation
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        // Update product’s average rating AFTER saving (even if PENDING, decide per policy)
        // We’ll only include APPROVED reviews in avg:
        updateProductAverageRating(product.getId());

        return mapToResponse(review);
    }

    // --- UPDATE ---
    @Transactional
    public ReviewResponse updateReview(String reviewId, ReviewRequest request) {
        String currentUserId = getCurrentUserId();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Authz: only owner can edit
        if (!review.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("You can only edit your own reviews.");
        }

        // Optional: prevent editing if status is REJECTED (business rule)
         if (review.getStatus() == Review.Status.REJECTED) {
             throw new RuntimeException("Rejected reviews cannot be edited.");
         }

        Product product = getProductById(request.getProductId());

        // Ensure same product (optional safety)
        if (!review.getProduct().getId().equals(product.getId())) {
            throw new RuntimeException("Review product mismatch.");
        }

        // Update fields
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());
        // Keep status as-is (e.g., PENDING → stays PENDING unless moderated)

        review = reviewRepository.save(review);

        // Update avg rating
        updateProductAverageRating(product.getId());

        return mapToResponse(review);
    }

    // --- DELETE ---
    @Transactional
    public void deleteReview(String reviewId) {
        String currentUserId = getCurrentUserId();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Authz: only owner (or admin) can delete
        if (!review.getUser().getId().equals(currentUserId)) {
            // Optional: allow ADMIN role to delete
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!(auth.getPrincipal() instanceof UserPrincipal up) || !up.hasRole("ADMIN")) {
                throw new RuntimeException("You can only delete your own reviews.");
            }
        }

        String productId = review.getProduct().getId();
        reviewRepository.delete(review);
        updateProductAverageRating(productId);
    }

    // --- GET BY PRODUCT ---
    public Page<ReviewResponse> getReviewsByProduct(String productId, Pageable pageable) {
        // Only show APPROVED reviews to public
        List<Review> reviews = reviewRepository.findByProduct_IdAndStatusOrderByCreatedAtDesc(
                productId, Review.Status.APPROVED
        );

        // Paginate manually (or use Aggregation for better perf at scale)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reviews.size());
        List<ReviewResponse> pageContent = reviews.subList(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, reviews.size());
    }

    // --- GET BY USER ---
    public List<ReviewResponse> getReviewsByUser(String userId) {
        // Allow user to see own reviews (any status), others only APPROVED
        String currentUserId = getCurrentUserId();
        List<Review> reviews;
        if (currentUserId.equals(userId)) {
            reviews = reviewRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        } else {
            reviews = reviewRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(
                    userId, Review.Status.APPROVED
            );
        }
        return reviews.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // --- UPDATE PRODUCT AVG RATING ---
    public void updateProductAverageRating(String productId) {
        Product product = getProductById(productId);

        // Get all APPROVED reviews for this product
        List<Review> approvedReviews = reviewRepository.findByProduct_IdAndStatus(
                productId, Review.Status.APPROVED
        );

        if (approvedReviews.isEmpty()) {
            product.setAverageRating(null);
            product.setTotalReviews(0);
        } else {
            double sum = approvedReviews.stream()
                    .mapToInt(Review::getRating)
                    .sum();
            double avg = sum / approvedReviews.size();

            product.setAverageRating(avg);
            product.setTotalReviews(approvedReviews.size());
        }

        productRepository.save(product);
    }

    // --- MAPPER ---
    private ReviewResponse mapToResponse(Review review) {
        return ReviewMapperUtil.toResponse(review);
    }

    // --- Moderate Review ---
    @Transactional
    public ReviewResponse moderateReview(String reviewId, Review.Status newStatus) {
        // Only MODERATOR/ADMIN reach here (enforced by SecurityConfig)
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (review.getStatus() == newStatus) {
            return mapToResponse(review); // no-op if same status
        }

        String productId = review.getProduct().getId();

        // Only allow transition to APPROVED or REJECTED from PENDING (optional stricter policy)
        if (review.getStatus() != Review.Status.PENDING) {
            throw new RuntimeException("Only PENDING reviews can be moderated.");
        }

        if (newStatus != Review.Status.APPROVED && newStatus != Review.Status.REJECTED) {
            throw new IllegalArgumentException("Invalid moderation status: " + newStatus);
        }

        // Update status & timestamp
        review.setStatus(newStatus);
        review.setUpdatedAt(LocalDateTime.now());
        review = reviewRepository.save(review);

        // Recalculate avg rating only if status changed to/from APPROVED
        updateProductAverageRating(productId);

        return mapToResponse(review);
    }
}