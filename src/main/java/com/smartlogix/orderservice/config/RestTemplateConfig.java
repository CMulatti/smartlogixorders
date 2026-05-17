package com.smartlogix.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**RestTemplate is the HTTP client we use to call other microservices.
 * @Bean: injects one shared instance everywhere. It saves us having to say "new RestTemplate()" in every single class.
 * We will replace this with Kafka in version 2.0 and we will change this config + the calls*/

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

