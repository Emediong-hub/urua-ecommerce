package com.urua.urua_backend.controller;

import com.urua.urua_backend.dto.CartItemRequest;
import com.urua.urua_backend.dto.CartResponse;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal User user) {
        return cartService.getCart(user);
    }

    @PostMapping("/items")
    public CartResponse addItem(@AuthenticationPrincipal User user,
                                @Valid @RequestBody CartItemRequest request) {
        return cartService.addItem(user, request);
    }

    @PutMapping("/items")
    public CartResponse updateQuantity(@AuthenticationPrincipal User user,
                                       @Valid @RequestBody CartItemRequest request) {
        return cartService.updateQuantity(user, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(@AuthenticationPrincipal User user,
                                   @PathVariable String productId) {
        return cartService.removeItem(user, productId);
    }

    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal User user) {
        return cartService.clear(user);
    }
}