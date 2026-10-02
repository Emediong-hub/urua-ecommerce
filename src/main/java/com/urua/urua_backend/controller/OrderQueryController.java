package com.urua.urua_backend.controller;

import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.dto.PageResponse;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {

    private final OrderQueryService orderQueryService;

    @GetMapping
    public PageResponse<OrderResponse> myOrders(@AuthenticationPrincipal User buyer,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return orderQueryService.getMyOrders(buyer, page, size);
    }

    @GetMapping("/{id}")
    public OrderResponse myOrder(@AuthenticationPrincipal User buyer, @PathVariable String id) {
        return orderQueryService.getMyOrder(buyer, id);
    }
}