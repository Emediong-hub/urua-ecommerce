package com.urua.urua_backend.service;

import com.mongodb.client.result.UpdateResult;
import com.urua.urua_backend.dto.CheckoutRequest;
import com.urua.urua_backend.dto.CheckoutResponse;
import com.urua.urua_backend.dto.OrderResponse;
import com.urua.urua_backend.model.Cart;
import com.urua.urua_backend.model.CartItem;
import com.urua.urua_backend.model.Order;
import com.urua.urua_backend.model.OrderItem;
import com.urua.urua_backend.model.OrderStatus;
import com.urua.urua_backend.model.Product;
import com.urua.urua_backend.model.ShippingAddress;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.CartRepository;
import com.urua.urua_backend.repository.OrderRepository;
import com.urua.urua_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;
    private final PaystackService paystackService;

    // Flat shipping fee in naira. Defaults to 0 until you set app.shipping.flat-fee in application.properties
    @Value("${app.shipping.flat-fee:0}")
    private BigDecimal flatShippingFee;

    public CheckoutResponse checkout(User buyer, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserId(buyer.getId())
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty"));

        List<String> ids = cart.getItems().stream().map(CartItem::getProductId).toList();
        Map<String, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Freeze each item's name and price at the moment of purchase
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Product product = products.get(item.getProductId());
            if (product == null || !product.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "An item in your cart is no longer available. Please review your cart.");
            }
            orderItems.add(new OrderItem(product.getId(), product.getName(), product.getPrice(),
                    item.getQuantity(), product.getSellerId(), product.getSellerName()));
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal total = subtotal.add(flatShippingFee);
        String reference = "URUA-" + UUID.randomUUID();

        reserveStock(orderItems);

        Order order = Order.builder()
                .buyerId(buyer.getId())
                .items(orderItems)
                .shippingAddress(new ShippingAddress(
                        request.getFullName().trim(),
                        request.getPhone().trim(),
                        request.getAddressLine().trim(),
                        request.getCity().trim(),
                        request.getState().trim()))
                .subtotal(subtotal)
                .shippingFee(flatShippingFee)
                .total(total)
                .status(OrderStatus.PENDING_PAYMENT)
                .paymentReference(reference)
                .build();

        String paymentUrl;
        try {
            order = orderRepository.save(order);
            paymentUrl = paystackService.initialize(buyer.getEmail(), total, reference);
        } catch (RuntimeException e) {
            // Undo everything: put the stock back and remove the unpaid order
            releaseStock(orderItems);
            if (order.getId() != null) {
                orderRepository.deleteById(order.getId());
            }
            throw e;
        }

        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return new CheckoutResponse(order.getId(), reference, paymentUrl, total);
    }

    // Asks Paystack whether the payment really happened, then marks the order PAID
    public OrderResponse verifyPayment(User buyer, String reference) {
        Order order = orderRepository.findByPaymentReference(reference)
                .filter(o -> o.getBuyerId().equals(buyer.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            PaystackService.PaymentVerification result = paystackService.verify(reference);

            // The amount must match exactly, so a cheaper payment can't unlock a bigger order
            if (result.success() && result.amountKobo() == toKobo(order.getTotal())) {
                // Only flips the status if the order is still PENDING_PAYMENT,
                // so two verify requests at the same time can't both process it
                Query query = new Query(Criteria.where("id").is(order.getId())
                        .and("status").is(OrderStatus.PENDING_PAYMENT));
                Update update = new Update()
                        .set("status", OrderStatus.PAID)
                        .set("paidAt", Instant.now());
                mongoTemplate.updateFirst(query, update, Order.class);

                order = orderRepository.findById(order.getId()).orElse(order);
            }
        }

        return toResponse(order);
    }

    private long toKobo(BigDecimal naira) {
        return naira.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
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

    // Takes stock one product at a time. The "stockQuantity >= qty" check lives inside the
    // database update itself, so two buyers can never both take the last item.
    private void reserveStock(List<OrderItem> items) {
        List<OrderItem> reserved = new ArrayList<>();

        for (OrderItem item : items) {
            Query query = new Query(Criteria.where("id").is(item.getProductId())
                    .and("active").is(true)
                    .and("stockQuantity").gte(item.getQuantity()));
            Update update = new Update().inc("stockQuantity", -item.getQuantity());

            UpdateResult result = mongoTemplate.updateFirst(query, update, Product.class);

            if (result.getModifiedCount() == 0) {
                releaseStock(reserved);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Not enough stock for " + item.getName());
            }
            reserved.add(item);
        }
    }

    private void releaseStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Query query = new Query(Criteria.where("id").is(item.getProductId()));
            Update update = new Update().inc("stockQuantity", item.getQuantity());
            mongoTemplate.updateFirst(query, update, Product.class);
        }
    }
}