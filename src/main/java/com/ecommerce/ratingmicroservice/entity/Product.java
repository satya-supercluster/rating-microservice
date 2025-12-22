package com.ecommerce.ratingmicroservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Product{

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Indexed
    @Field("category")
    private String category;

    @Field("price")
    private BigDecimal price;

    @Field("image_url")
    private String imageUrl;

    @Field("average_rating")
    private Double averageRating;

    @Field("total_reviews")
    private Integer totalReviews;
}