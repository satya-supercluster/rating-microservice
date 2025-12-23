package com.ecommerce.ratingmicroservice.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reviews")
@CompoundIndex(
        name = "product_user_idx",
        def = "{'product_id': 1, 'user_id': 1}",
        unique = true
)
public class Review{

    public enum Status {
        APPROVED, PENDING, REJECTED
    }

    @Id
    private String id;

    @DBRef
    @Field("product")
    private Product product;

    @DBRef
    @Field("user")
    private User user;

    @Field("rating")
    private Integer rating; // 1-5

    @Field("comment")
    private String comment;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("status")
    private Status status;

}