package com.urua.urua_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String buyerId;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private ShippingAddress shippingAddress;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal subtotal;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal shippingFee;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal total;

    private OrderStatus status;

    // The unique reference we give Paystack so we can find this order again
    @Indexed
    private String paymentReference;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant paidAt;
}