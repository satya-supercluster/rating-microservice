package com.ecommerce.ratingmicroservice.repository;

import com.ecommerce.ratingmicroservice.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    // Exists check for duplicate (product + user)
    boolean existsByProduct_IdAndUser_Id(String productId, String userId);

    // Get reviews by product (approved only, sorted newest first)
    List<Review> findByProduct_IdAndStatusOrderByCreatedAtDesc(String productId, Review.Status status);

    // Get reviews by user (any status, sorted newest first)
    List<Review> findByUser_IdOrderByCreatedAtDesc(String userId);

    // Optional: get a single review by product and user (for edit/delete validation)
    Optional<Review> findByProduct_IdAndUser_Id(String productId, String userId);

    // Custom query: get count of reviews by product (for avg recalc)
    long countByProduct_IdAndStatus(String productId, Review.Status status);

    // Custom query: get sum of ratings for a product (approved only)
    @Query(value = "{ 'product.$id': ?0, 'status': ?1 }",
            fields = "{ 'rating': 1 }")
    List<Review> findRatingsByProductAndStatus(String productId, Review.Status status);

    List<Review> findByUser_IdAndStatusOrderByCreatedAtDesc(String userId, Review.Status status);

    List<Review> findByProduct_IdAndStatus(String productId, Review.Status status);
}