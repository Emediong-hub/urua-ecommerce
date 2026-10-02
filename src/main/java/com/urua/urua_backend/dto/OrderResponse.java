package com.urua.urua_backend.dto;

import com.urua.urua_backend.model.OrderItem;
import com.urua.urua_backend.model.OrderStatus;
import com.urua.urua_backend.model.ShippingAddress;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private String id;
    private List<OrderItem> items;
    private ShippingAddress shippingAddress;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private OrderStatus status;
    private String paymentReference;
    private Instant createdAt;
    private Instant paidAt;
}