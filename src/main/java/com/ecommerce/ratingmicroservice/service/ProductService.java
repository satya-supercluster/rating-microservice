package com.ecommerce.ratingmicroservice.service;

import com.ecommerce.ratingmicroservice.dto.request.ProductRequest;
import com.ecommerce.ratingmicroservice.entity.Product;
import com.ecommerce.ratingmicroservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Product createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        return productRepository.save(product);
    }

    public Product updateProduct(String id, ProductRequest request) {
        return productRepository.findById(id)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setDescription(request.getDescription());
                    existing.setCategory(request.getCategory());
                    existing.setPrice(request.getPrice());
                    existing.setImageUrl(request.getImageUrl());
                    return productRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    // Search products with flexible criteria
    public Page<Product> searchProducts(
            String name,
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        String nameRegex = (name != null && !name.trim().isEmpty()) ? name : "";
        String categoryRegex = (category != null && !category.trim().isEmpty()) ? category : "";

        // Default price bounds if null
        BigDecimal lower = Optional.ofNullable(minPrice).orElse(BigDecimal.ZERO);
        BigDecimal upper = Optional.ofNullable(maxPrice).orElse(BigDecimal.valueOf(Long.MAX_VALUE));

        return productRepository.searchProducts(nameRegex, categoryRegex, lower, upper, pageable);
    }
}