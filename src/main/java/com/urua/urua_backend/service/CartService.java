package com.urua.urua_backend.service;

import com.urua.urua_backend.dto.CartItemRequest;
import com.urua.urua_backend.dto.CartItemResponse;
import com.urua.urua_backend.dto.CartResponse;
import com.urua.urua_backend.model.Cart;
import com.urua.urua_backend.model.CartItem;
import com.urua.urua_backend.model.Product;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.CartRepository;
import com.urua.urua_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartResponse getCart(User user) {
        return toResponse(findOrCreate(user));
    }

    public CartResponse addItem(User user, CartItemRequest request) {
        Product product = getActiveProduct(request.getProductId());
        Cart cart = findOrCreate(user);

        CartItem existing = findItem(cart, request.getProductId());
        int newQuantity = request.getQuantity() + (existing == null ? 0 : existing.getQuantity());
        checkStock(product, newQuantity);

        if (existing == null) {
            cart.getItems().add(new CartItem(product.getId(), newQuantity));
        } else {
            existing.setQuantity(newQuantity);
        }
        return save(cart);
    }

    // Sets the quantity to exactly the number given
    public CartResponse updateQuantity(User user, CartItemRequest request) {
        Product product = getActiveProduct(request.getProductId());
        Cart cart = findOrCreate(user);

        CartItem existing = findItem(cart, request.getProductId());
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item is not in your cart");
        }

        checkStock(product, request.getQuantity());
        existing.setQuantity(request.getQuantity());
        return save(cart);
    }

    public CartResponse removeItem(User user, String productId) {
        Cart cart = findOrCreate(user);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        return save(cart);
    }

    public CartResponse clear(User user) {
        Cart cart = findOrCreate(user);
        cart.getItems().clear();
        return save(cart);
    }

    private Cart findOrCreate(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> Cart.builder().userId(user.getId()).build());
    }

    private CartItem findItem(Cart cart, String productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private Product getActiveProduct(String productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void checkStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only " + product.getStockQuantity() + " of " + product.getName() + " in stock");
        }
    }

    private CartResponse save(Cart cart) {
        cart.setUpdatedAt(Instant.now());
        return toResponse(cartRepository.save(cart));
    }

    // Names, prices and stock are looked up fresh every time, so the cart never shows stale prices
    private CartResponse toResponse(Cart cart) {
        List<String> ids = cart.getItems().stream().map(CartItem::getProductId).toList();
        Map<String, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cart.getItems()) {
            Product product = products.get(item.getProductId());
            boolean available = product != null && product.isActive()
                    && product.getStockQuantity() >= item.getQuantity();

            BigDecimal price = product != null ? product.getPrice() : BigDecimal.ZERO;
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

            String imageUrl = null;
            if (product != null && product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                imageUrl = product.getImageUrls().get(0);
            }

            items.add(CartItemResponse.builder()
                    .productId(item.getProductId())
                    .name(product != null ? product.getName() : "Product no longer available")
                    .imageUrl(imageUrl)
                    .price(price)
                    .quantity(item.getQuantity())
                    .lineTotal(lineTotal)
                    .stockAvailable(product != null ? product.getStockQuantity() : 0)
                    .available(available)
                    .build());

            if (available) {
                subtotal = subtotal.add(lineTotal);
                totalItems += item.getQuantity();
            }
        }

        return new CartResponse(items, totalItems, subtotal);
    }
}