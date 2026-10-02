package com.urua.urua_backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @TextIndexed(weight = 3)
    private String name;

    @TextIndexed
    private String description;

    // Stored as a true decimal number in MongoDB so sorting and filtering by price work
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    @Indexed
    private String category;

    private int stockQuantity;

    private List<String> imageUrls;

    @Indexed
    private String sellerId;

    private String sellerName;

    // Which of Nigeria's 36 states (or FCT) the seller ships from
    @Indexed
    private String state;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private Instant createdAt = Instant.now();
}