package com.urua.urua_backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

// A frozen copy of the product at the moment of purchase, so later price changes don't rewrite old orders
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private String name;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private int quantity;
    private String sellerId;
    private String sellerName;
}