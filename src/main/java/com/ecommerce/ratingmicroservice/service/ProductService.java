package com.ecommerce.ratingmicroservice.service;

import com.ecommerce.ratingmicroservice.dto.request.ProductRequest;
import com.ecommerce.ratingmicroservice.dto.response.PageResponse;
import com.ecommerce.ratingmicroservice.dto.response.ProductResponse;
import com.ecommerce.ratingmicroservice.entity.Product;
import com.ecommerce.ratingmicroservice.mapper.ProductMapperUtil;
import com.ecommerce.ratingmicroservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Product service with Redis caching layer.
 * All cached methods return DTOs, never MongoDB entities or Spring Page objects.
 * Cache keys include all query parameters for deterministic cache hits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Create product - evicts all list/search caches since new product affects results
     */
    @Caching(evict = {
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = ProductMapperUtil.toEntity(request);

        Product saved = productRepository.save(product);
        log.info("Created product with ID: {}", saved.getId());

        return ProductResponse.from(saved);
    }

    /**
     * Update product - evicts specific product cache and all list/search caches
     */
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    @Transactional
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setCategory(request.getCategory());
        existing.setPrice(request.getPrice());
        existing.setImageUrl(request.getImageUrl());

        Product updated = productRepository.save(existing);
        log.info("Updated product with ID: {}", id);

        return ProductResponse.from(updated);
    }

    /**
     * Delete product - evicts specific product cache and all list/search caches
     */
    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    @Transactional
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        log.info("Deleted product with ID: {}", id);
    }

    /**
     * Get product by ID - cached with simple key
     * Cache key: products::<productId>
     */
    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public ProductResponse getProductById(String id) {
        log.debug("Fetching product from DB: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));

        return ProductResponse.from(product);
    }

    /**
     * Get all products with pagination - cached with deterministic key
     * Cache key: productList::<page>:<size>:<sortBy>:<direction>
     *
     * Note: We cache PageResponse<ProductResponse>, not Spring's Page
     */
    @Cacheable(
            value = "productList",
            key = "#pageable.pageNumber + ':' + #pageable.pageSize + ':' + " +
                    "#pageable.sort.toString()",
            unless = "#result == null || #result.empty"
    )
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        log.debug("Fetching product list from DB: page={}, size={}, sort={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        Page<Product> productPage = productRepository.findAll(pageable);

        // Convert MongoDB entities to DTOs
        Page<ProductResponse> responsePage = productPage.map(ProductResponse::from);

        // Convert Spring Page to cacheable PageResponse
        return PageResponse.from(responsePage);
    }

    /**
     * Search products with flexible criteria - cached with all parameters
     * Cache key: productSearch::<name>:<category>:<minPrice>:<maxPrice>:<page>:<size>:<sort>
     *
     * Includes all query filters and pagination params for deterministic cache hits
     */
    @Cacheable(
            value = "productSearch",
            key = "(#name ?: 'null') + ':' + " +
                    "(#category ?: 'null') + ':' + " +
                    "(#minPrice ?: 'null') + ':' + " +
                    "(#maxPrice ?: 'null') + ':' + " +
                    "#pageable.pageNumber + ':' + " +
                    "#pageable.pageSize + ':' + " +
                    "#pageable.sort.toString()",
            unless = "#result == null || #result.empty"
    )
    public PageResponse<ProductResponse> searchProducts(
            String name,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        log.debug("Searching products from DB: name={}, category={}, minPrice={}, maxPrice={}",
                name, category, minPrice, maxPrice);

        String nameRegex = (name != null && !name.trim().isEmpty()) ? name : "";
        String categoryRegex = (category != null && !category.trim().isEmpty()) ? category : "";

        BigDecimal lower = Optional.ofNullable(minPrice).orElse(BigDecimal.ZERO);
        BigDecimal upper = Optional.ofNullable(maxPrice).orElse(BigDecimal.valueOf(Long.MAX_VALUE));

        Page<Product> productPage = productRepository.searchProducts(
                nameRegex, categoryRegex, lower, upper, pageable
        );

        // Convert MongoDB entities to DTOs
        Page<ProductResponse> responsePage = productPage.map(ProductResponse::from);

        // Convert Spring Page to cacheable PageResponse
        return PageResponse.from(responsePage);
    }

    /**
     * Internal method for updating product ratings (called by ReviewService)
     * Evicts the specific product cache to ensure fresh data on next fetch
     */
    @CacheEvict(value = "products", key = "#productId")
    @Transactional
    public void updateProductRating(String productId, Double averageRating, Integer totalReviews) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        product.setAverageRating(averageRating);
        product.setTotalReviews(totalReviews);
        productRepository.save(product);

        log.info("Updated rating for product {}: avg={}, total={}",
                productId, averageRating, totalReviews);
    }
}