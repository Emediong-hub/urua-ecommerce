package com.urua.urua_backend.service;

import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.dto.PageResponse;
import com.urua.urua_backend.model.Order;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public PageResponse<OrderResponse> getMyOrders(User buyer, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> result = orderRepository.findByBuyerId(buyer.getId(), pageable);
        List<OrderResponse> content = result.getContent().stream().map(this::toResponse).toList();

        return new PageResponse<>(content, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    // Buyers can only open their own orders. Someone else's order looks the same as a missing one.
    public OrderResponse getMyOrder(User buyer, String orderId) {
        Order order = orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyer.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .items(order.getItems())
                .shippingAddress(order.getShippingAddress())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .status(order.getStatus())
                .paymentReference(order.getPaymentReference())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }
}