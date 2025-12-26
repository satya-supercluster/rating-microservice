package com.ecommerce.ratingmicroservice.dto.response;

import com.ecommerce.ratingmicroservice.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {

    private String id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private String imageUrl;
    private Double averageRating;
    private Integer totalReviews;

    /**
     * Factory method to convert MongoDB entity to cacheable DTO
     */
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getImageUrl(),
                product.getAverageRating() != null ? product.getAverageRating() : 0.0,
                product.getTotalReviews() != null ? product.getTotalReviews() : 0
        );
    }
}