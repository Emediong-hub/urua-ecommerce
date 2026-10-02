package com.urua.urua_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class PaystackService {

    public record PaymentVerification(boolean success, long amountKobo) {}

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final String callbackUrl;

    public PaystackService(@Value("${app.paystack.secret-key:}") String secretKey,
                           @Value("${app.paystack.callback-url:http://localhost:5173/payment/callback}") String callbackUrl) {
        this.callbackUrl = callbackUrl;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .build();
    }

    // Starts a payment and returns the Paystack page the buyer should be sent to
    public String initialize(String email, BigDecimal amountNaira, String reference) {
        // Paystack wants the amount in kobo (1 naira = 100 kobo)
        long amountKobo = amountNaira.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        Map<String, Object> body = Map.of(
                "email", email,
                "amount", amountKobo,
                "reference", reference,
                "callback_url", callbackUrl
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/transaction/initialize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);

            Map<?, ?> data = (Map<?, ?>) response.get("data");
            return (String) data.get("authorization_url");
        } catch (RestClientException | NullPointerException | ClassCastException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not start payment. Please try again.");
        }
    }

    // Asks Paystack directly whether this payment really went through
    public PaymentVerification verify(String reference) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/transaction/verify/{reference}", reference)
                    .retrieve()
                    .body(MAP_TYPE);

            Map<?, ?> data = (Map<?, ?>) response.get("data");
            boolean success = "success".equals(data.get("status"));
            long amountKobo = ((Number) data.get("amount")).longValue();
            return new PaymentVerification(success, amountKobo);
        } catch (RestClientException | NullPointerException | ClassCastException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not verify payment. Please try again.");
        }
    }
}