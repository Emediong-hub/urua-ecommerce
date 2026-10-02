package com.urua.urua_backend.repository;

import com.urua.urua_backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findBySellerId(String sellerId, Pageable pageable);
}