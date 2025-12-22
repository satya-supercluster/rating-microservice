package com.ecommerce.ratingmicroservice.repository;

import com.ecommerce.ratingmicroservice.entity.Review;
import com.ecommerce.ratingmicroservice.entity.Product;
import com.ecommerce.ratingmicroservice.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends MongoRepository<Review, String> {

    // Enforces unique (product, user) constraint at query level
    Optional<Review> findByProductAndUser(Product product, User user);

    // Fetch all reviews for a product
    List<Review> findByProduct(Product product);

    // Fetch all reviews by a user
    List<Review> findByUser(User user);

    // Fetch reviews by product and status
    List<Review> findByProductAndStatus(Product product, Review.Status status);

    // Fetch reviews by user and status
    List<Review> findByUserAndStatus(User user, Review.Status status);
}
