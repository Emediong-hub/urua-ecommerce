package com.urua.urua_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private String orderId;
    private String paymentReference;
    private String paymentUrl;
    private BigDecimal total;
}