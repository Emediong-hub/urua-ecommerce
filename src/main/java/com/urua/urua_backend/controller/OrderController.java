package com.urua.urua_backend.controller;

import com.urua.urua_backend.dto.CheckoutRequest;
import com.urua.urua_backend.dto.CheckoutResponse;
import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse checkout(@AuthenticationPrincipal User buyer,
                                     @Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(buyer, request);
    }

    @PostMapping("/verify/{reference}")
    public OrderResponse verify(@AuthenticationPrincipal User buyer,
                                @PathVariable String reference) {
        return orderService.verifyPayment(buyer, reference);
    }
}