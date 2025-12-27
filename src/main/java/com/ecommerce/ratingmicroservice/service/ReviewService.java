package com.ecommerce.ratingmicroservice.service;

import com.ecommerce.ratingmicroservice.dto.request.ReviewRequest;
import com.ecommerce.ratingmicroservice.dto.response.PageResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

/**
 * Review service with Redis caching layer.
 * All cached methods return DTOs for safe Redis serialization.
 * Cache keys include all query parameters for deterministic cache hits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

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

    /**
     * Create review - evicts product-specific and user-specific review caches
     */
    @Caching(evict = {
            @CacheEvict(value = "reviewsByProduct", key = "#request.productId + ':*'", allEntries = false),
            @CacheEvict(value = "reviewsByUser", allEntries = true)
    })
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
        review.setStatus(Review.Status.PENDING);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);
        log.info("Created review with ID: {} for product: {}", review.getId(), product.getId());

        // Update product's average rating
        updateProductAverageRating(product.getId());

        return mapToResponse(review);
    }

    /**
     * Update review - evicts specific review, product reviews, and user reviews caches
     */
    @Caching(evict = {
            @CacheEvict(value = "reviews", key = "#reviewId"),
            @CacheEvict(value = "reviewsByProduct", allEntries = true),
            @CacheEvict(value = "reviewsByUser", allEntries = true)
    })
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

        review = reviewRepository.save(review);
        log.info("Updated review with ID: {}", reviewId);

        // Update avg rating
        updateProductAverageRating(product.getId());

        return mapToResponse(review);
    }

    /**
     * Delete review - evicts specific review, product reviews, and user reviews caches
     */
    @Caching(evict = {
            @CacheEvict(value = "reviews", key = "#reviewId"),
            @CacheEvict(value = "reviewsByProduct", allEntries = true),
            @CacheEvict(value = "reviewsByUser", allEntries = true)
    })
    @Transactional
    public void deleteReview(String reviewId) {
        String currentUserId = getCurrentUserId();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Authz: only owner (or admin) can delete
        if (!review.getUser().getId().equals(currentUserId)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!(auth.getPrincipal() instanceof UserPrincipal up) || !up.hasRole("ADMIN")) {
                throw new RuntimeException("You can only delete your own reviews.");
            }
        }

        String productId = review.getProduct().getId();
        reviewRepository.delete(review);
        log.info("Deleted review with ID: {}", reviewId);

        updateProductAverageRating(productId);
    }

    /**
     * Get single review by ID - cached with simple key
     * Cache key: reviews::<reviewId>
     */
    @Cacheable(value = "reviews", key = "#reviewId", unless = "#result == null")
    public ReviewResponse getReviewById(String reviewId) {
        log.debug("Fetching review from DB: {}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        return mapToResponse(review);
    }

    /**
     * Get reviews by product with pagination - cached with deterministic key
     * Cache key: reviewsByProduct::<productId>:<page>:<size>:<sort>
     * Only shows APPROVED reviews to public
     */
    @Cacheable(
            value = "reviewsByProduct",
            key = "#productId + ':' + #pageable.pageNumber + ':' + " +
                    "#pageable.pageSize + ':' + #pageable.sort.toString()",
            unless = "#result == null || #result.empty"
    )
    public PageResponse<ReviewResponse> getReviewsByProduct(String productId, Pageable pageable) {
        log.debug("Fetching reviews from DB for product: {}, page={}, size={}",
                productId, pageable.getPageNumber(), pageable.getPageSize());

        // Only show APPROVED reviews to public
        List<Review> reviews = reviewRepository.findByProduct_IdAndStatusOrderByCreatedAtDesc(
                productId, Review.Status.APPROVED
        );

        // Paginate manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reviews.size());
        List<ReviewResponse> pageContent = reviews.subList(start, end).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        Page<ReviewResponse> page = new PageImpl<>(pageContent, pageable, reviews.size());

        // Convert to cacheable PageResponse
        return PageResponse.from(page);
    }

    /**
     * Get reviews by user - cached with deterministic key
     * Cache key: reviewsByUser::<userId>:<currentUserId>
     *
     * Users can see all their own reviews (any status)
     * Others can only see APPROVED reviews
     */
    @Cacheable(
            value = "reviewsByUser",
            key = "#userId + ':' + T(org.springframework.security.core.context.SecurityContextHolder)" +
                    ".getContext().getAuthentication().getPrincipal().getId()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<ReviewResponse> getReviewsByUser(String userId) {
        log.debug("Fetching reviews from DB for user: {}", userId);

        String currentUserId = getCurrentUserId();
        List<Review> reviews;

        if (currentUserId.equals(userId)) {
            reviews = reviewRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        } else {
            reviews = reviewRepository.findByUser_IdAndStatusOrderByCreatedAtDesc(
                    userId, Review.Status.APPROVED
            );
        }

        return reviews.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update product average rating - evicts product cache to ensure fresh data
     * This method is called internally after review operations
     */
    @CacheEvict(value = "products", key = "#productId")
    @Transactional
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
        log.info("Updated rating for product {}: avg={}, total={}",
                productId, product.getAverageRating(), product.getTotalReviews());
    }

    /**
     * Moderate review - evicts multiple caches as it affects visibility
     * Only MODERATOR/ADMIN can access this (enforced by SecurityConfig)
     */
    @Caching(evict = {
            @CacheEvict(value = "reviews", key = "#reviewId"),
            @CacheEvict(value = "reviewsByProduct", allEntries = true),
            @CacheEvict(value = "reviewsByUser", allEntries = true)
    })
    @Transactional
    public ReviewResponse moderateReview(String reviewId, Review.Status newStatus) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        if (review.getStatus() == newStatus) {
            return mapToResponse(review); // no-op if same status
        }

        String productId = review.getProduct().getId();

        // Only allow transition to APPROVED or REJECTED from PENDING
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
        log.info("Moderated review {} to status: {}", reviewId, newStatus);

        // Recalculate avg rating
        updateProductAverageRating(productId);

        return mapToResponse(review);
    }

    // --- MAPPER ---
    private ReviewResponse mapToResponse(Review review) {
        return ReviewMapperUtil.toResponse(review);
    }
}