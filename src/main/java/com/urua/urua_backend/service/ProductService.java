package com.urua.urua_backend.service;

import com.urua.urua_backend.dto.PageResponse;
import com.urua.urua_backend.dto.ProductRequest;
import com.urua.urua_backend.dto.ProductResponse;
import com.urua.urua_backend.model.Product;
import com.urua.urua_backend.model.User;
import com.urua.urua_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    public ProductResponse create(User seller, ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .price(request.getPrice())
                .category(request.getCategory().trim())
                .stockQuantity(request.getStockQuantity())
                .imageUrls(request.getImageUrls())
                .sellerId(seller.getId())
                .sellerName(seller.getFullName())
                .state(request.getState().trim())
                .build();

        return toResponse(productRepository.save(product));
    }

    public PageResponse<ProductResponse> search(String q, String category, String state,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                int page, int size) {
        List<Criteria> filters = new ArrayList<>();
        filters.add(Criteria.where("active").is(true));

        if (q != null && !q.isBlank()) {
            String pattern = Pattern.quote(q.trim());
            filters.add(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern, "i"),
                    Criteria.where("description").regex(pattern, "i")));
        }
        if (category != null && !category.isBlank()) {
            filters.add(Criteria.where("category").regex("^" + Pattern.quote(category.trim()) + "$", "i"));
        }
        if (state != null && !state.isBlank()) {
            filters.add(Criteria.where("state").regex("^" + Pattern.quote(state.trim()) + "$", "i"));
        }
        if (minPrice != null) {
            filters.add(Criteria.where("price").gte(minPrice));
        }
        if (maxPrice != null) {
            filters.add(Criteria.where("price").lte(maxPrice));
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Query query = new Query(new Criteria().andOperator(filters));
        long total = mongoTemplate.count(query, Product.class);
        List<Product> items = mongoTemplate.find(query.with(pageable), Product.class);

        List<ProductResponse> content = items.stream().map(this::toResponse).toList();
        int totalPages = (int) Math.ceil((double) total / safeSize);

        return new PageResponse<>(content, safePage, safeSize, total, totalPages);
    }

    public ProductResponse getById(String id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return toResponse(product);
    }

    public PageResponse<ProductResponse> getMine(User seller, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> result = productRepository.findBySellerId(seller.getId(), pageable);
        List<ProductResponse> content = result.getContent().stream().map(this::toResponse).toList();

        return new PageResponse<>(content, safePage, safeSize, result.getTotalElements(), result.getTotalPages());
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .category(p.getCategory())
                .stockQuantity(p.getStockQuantity())
                .imageUrls(p.getImageUrls())
                .sellerId(p.getSellerId())
                .sellerName(p.getSellerName())
                .state(p.getState())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}