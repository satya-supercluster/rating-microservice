package com.ecommerce.ratingmicroservice.repository;

import com.ecommerce.ratingmicroservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.math.BigDecimal;

public interface ProductRepository extends MongoRepository<Product, String> {

    // Search by name (case-insensitive, partial match)
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Search by category
    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    // Search by name + category
    Page<Product> findByNameContainingIgnoreCaseAndCategoryIgnoreCase(String name, String category, Pageable pageable);

    // Search by price range
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Combined search: name, category, price range
    @Query("{" +
            "  $and: [" +
            "    { 'name': { $regex: ?0, $options: 'i' } }," +
            "    { 'category': { $regex: ?1, $options: 'i' } }," +
            "    { 'price': { $gte: ?2, $lte: ?3 } }" +
            "  ]" +
            "}")
    Page<Product> searchProducts(String nameRegex, String categoryRegex, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}