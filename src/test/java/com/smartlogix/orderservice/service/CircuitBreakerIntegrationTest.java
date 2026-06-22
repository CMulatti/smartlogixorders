package com.smartlogix.orderservice.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
public class CircuitBreakerIntegrationTest {

    @Autowired
    ExternalApiService externalApiService;  //REAL bean with CB proxy

    @Autowired
    CircuitBreakerRegistry registry;

    @MockitoBean
    RestTemplate restTemplate;  //mock the HTTP client inside it

    @BeforeEach
    void reset() {
        registry.circuitBreaker("productService").reset();
    }

    @Test
    void whenProductServiceFails_circuitBreakerOpens() {

        //Mock RestTemplate to fail (ExternalApiService is real, RestTemplate inside it fails)
        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        for (int i = 0; i < 5; i++) {
            try {
                externalApiService.notifyProductService(
                        "http://localhost:8082/products/reduce-stock",
                        Map.of("productId", 1L, "quantity", 3)
                );
            } catch (Exception e) {
                System.out.println("Call " + (i+1) + ": " + e.getMessage());
            }
        }

        CircuitBreaker cb = registry.circuitBreaker("productService");
        System.out.println("State: " + cb.getState());
        System.out.println("Calls: " + cb.getMetrics().getNumberOfBufferedCalls()); //how many calls Resilience4j has stored in the sliding window
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}

