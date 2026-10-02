package com.urua.urua_backend.service;

import com.mongodb.client.result.UpdateResult;
import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.dto.PageResponse;
import com.urua.urua_backend.model.Order;
import com.urua.urua_backend.model.OrderStatus;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FulfillmentService {

    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    // Orders that contain this seller's products. Unpaid and cancelled orders are hidden from sellers.
    public PageResponse<OrderResponse> getSellerOrders(User seller, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Query query = new Query(Criteria.where("items.sellerId").is(seller.getId())
                .and("status").in(List.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED)));

        long total = mongoTemplate.count(query, Order.class);
        List<Order> orders = mongoTemplate.find(query.with(pageable), Order.class);
        List<OrderResponse> content = orders.stream().map(this::toResponse).toList();
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return new PageResponse<>(content, safePage, safeSize, total, totalPages);
    }

    // Seller marks a paid order as shipped
    public OrderResponse ship(User seller, String orderId) {
        orderRepository.findById(orderId)
                .filter(o -> o.getItems().stream().anyMatch(i -> seller.getId().equals(i.getSellerId())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        move(orderId, OrderStatus.PAID, OrderStatus.SHIPPED, "Only paid orders can be shipped");
        return toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    // Buyer confirms the goods arrived
    public OrderResponse confirmDelivery(User buyer, String orderId) {
        orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyer.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        move(orderId, OrderStatus.SHIPPED, OrderStatus.DELIVERED, "Only shipped orders can be confirmed as delivered");
        return toResponse(orderRepository.findById(orderId).orElseThrow());
    }

    // Changes the status only if the order is currently in the expected status
    private void move(String orderId, OrderStatus from, OrderStatus to, String errorMessage) {
        Query query = new Query(Criteria.where("id").is(orderId).and("status").is(from));
        UpdateResult result = mongoTemplate.updateFirst(query, new Update().set("status", to), Order.class);

        if (result.getModifiedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
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