package com.urua.urua_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private String productId;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private int quantity;
    private BigDecimal lineTotal;
    private int stockAvailable;
    // false if the product was removed or has run out of stock
    private boolean available;
}