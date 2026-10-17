package com.urua.urua_backend.service;

import com.urua.urua_backend.model.Order;
import com.urua.urua_backend.model.OrderItem;
import com.urua.urua_backend.model.OrderStatus;
import com.urua.urua_backend.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupJob {

    private final MongoTemplate mongoTemplate;
    private final PaystackService paystackService;

    // How long an order may stay unpaid before we look at it
    @Value("${app.orders.expiry-minutes:30}")
    private long expiryMinutes;

    // Runs every 5 minutes by default
    @Scheduled(fixedDelayString = "${app.orders.cleanup-interval-ms:300000}",
            initialDelayString = "${app.orders.cleanup-interval-ms:300000}")
    public void cleanUpUnpaidOrders() {
        Instant cutoff = Instant.now().minus(expiryMinutes, ChronoUnit.MINUTES);

        Query stale = new Query(Criteria.where("status").is(OrderStatus.PENDING_PAYMENT)
                .and("createdAt").lt(cutoff));

        List<Order> orders = mongoTemplate.find(stale, Order.class);

        for (Order order : orders) {
            try {
                handle(order);
            } catch (RuntimeException e) {
                // Paystack unreachable or similar. Leave this order for the next run.
                log.warn("Could not check order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void handle(Order order) {
        PaystackService.PaymentVerification result = paystackService.verify(order.getPaymentReference());

        if (result.success()) {
            if (result.amountKobo() == toKobo(order.getTotal())) {
                // The buyer paid but never came back to our site, so complete the order now
                if (markPaid(order.getId())) {
                    log.info("Order {} was paid on Paystack, marked as PAID", order.getId());
                }
            } else {
                log.warn("Order {} has a payment with a different amount, needs a manual check", order.getId());
            }
            return;
        }

        if ("abandoned".equals(result.status()) || "failed".equals(result.status())) {
            // The status only changes if the order is still unpaid, so stock is released once
            if (markCancelled(order.getId())) {
                releaseStock(order.getItems());
                log.info("Cancelled unpaid order {} and released its stock", order.getId());
            }
        }
        // Any other status (for example a payment still processing) is left for the next run
    }

    private boolean markPaid(String orderId) {
        Query query = new Query(Criteria.where("id").is(orderId)
                .and("status").is(OrderStatus.PENDING_PAYMENT));
        Update update = new Update()
                .set("status", OrderStatus.PAID)
                .set("paidAt", Instant.now());
        return mongoTemplate.updateFirst(query, update, Order.class).getModifiedCount() == 1;
    }

    private boolean markCancelled(String orderId) {
        Query query = new Query(Criteria.where("id").is(orderId)
                .and("status").is(OrderStatus.PENDING_PAYMENT));
        Update update = new Update().set("status", OrderStatus.CANCELLED);
        return mongoTemplate.updateFirst(query, update, Order.class).getModifiedCount() == 1;
    }

    private void releaseStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Query query = new Query(Criteria.where("id").is(item.getProductId()));
            Update update = new Update().inc("stockQuantity", item.getQuantity());
            mongoTemplate.updateFirst(query, update, Product.class);
        }
    }

    private long toKobo(BigDecimal naira) {
        return naira.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}