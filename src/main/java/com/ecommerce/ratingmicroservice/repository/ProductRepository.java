package com.ecommerce.ratingmicroservice.repository;

import com.ecommerce.ratingmicroservice.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByCategory(String category);
}
