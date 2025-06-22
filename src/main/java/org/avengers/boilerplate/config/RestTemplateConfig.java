package org.avengers.boilerplate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Value("${ssl.monitor.webhook.timeout:5000}")
    private int webhookTimeout;
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(webhookTimeout);
        factory.setReadTimeout(webhookTimeout);
        
        return new RestTemplate(factory);
    }
} 