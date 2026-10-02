package com.urua.urua_backend.controller;

import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.dto.PageResponse;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.service.FulfillmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    @GetMapping("/api/seller/orders")
    public PageResponse<OrderResponse> sellerOrders(@AuthenticationPrincipal User seller,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return fulfillmentService.getSellerOrders(seller, page, size);
    }

    @PutMapping("/api/seller/orders/{id}/ship")
    public OrderResponse ship(@AuthenticationPrincipal User seller, @PathVariable String id) {
        return fulfillmentService.ship(seller, id);
    }

    @PostMapping("/api/orders/{id}/confirm-delivery")
    public OrderResponse confirmDelivery(@AuthenticationPrincipal User buyer, @PathVariable String id) {
        return fulfillmentService.confirmDelivery(buyer, id);
    }
}