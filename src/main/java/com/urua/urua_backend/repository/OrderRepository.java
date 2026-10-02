package com.urua.urua_backend.repository;

import com.urua.urua_backend.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByPaymentReference(String paymentReference);

    Page<Order> findByBuyerId(String buyerId, Pageable pageable);
}