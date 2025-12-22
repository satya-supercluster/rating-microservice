package com.ecommerce.ratingmicroservice.mapper;

import com.ecommerce.ratingmicroservice.dto.request.ProductRequest;
import com.ecommerce.ratingmicroservice.entity.Product;

public class ProductMapperUtil {

    private ProductMapperUtil() {}

    public static Product toEntity(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        product.setAverageRating(0.0);
        product.setTotalReviews(0);
        return product;
    }
}
