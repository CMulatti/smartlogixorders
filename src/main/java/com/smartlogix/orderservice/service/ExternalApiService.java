package com.smartlogix.orderservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalApiService {
    private final RestTemplate restTemplate;

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackProduct")
    public void notifyProductService(String url, Map<String, Object> stockRequest) {
        System.out.println("Attempting HTTP call...");
        restTemplate.postForObject(url, stockRequest, Object.class);
    }

    public void fallbackProduct(String url, Map<String, Object> stockRequest, Throwable ex) {
        System.err.println("[OrderService] Error llamando a ProductService. Razón: " + ex.getMessage());
        throw new RuntimeException("Ups! No es posible actualizar stock en este momento. Por favor intente más tarde.");
    }
}
