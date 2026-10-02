package com.urua.urua_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private int stockQuantity;
    private List<String> imageUrls;
    private String sellerId;
    private String sellerName;
    private String state;
    private boolean active;
    private Instant createdAt;
}