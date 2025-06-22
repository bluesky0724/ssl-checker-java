package org.avengers.boilerplate.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.repository.DomainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!prod")
public class DataLoader implements CommandLineRunner {
    
    private final DomainRepository domainRepository;
    
    @Override
    public void run(String... args) throws Exception {
        if (domainRepository.count() == 0) {
            log.info("Loading sample domains for testing...");
            loadSampleDomains();
            log.info("Sample domains loaded successfully!");
        } else {
            log.info("Database already contains domains, skipping sample data load.");
        }
    }
    
    private void loadSampleDomains() {
        List<Domain> sampleDomains = Arrays.asList(
                Domain.builder()
                        .name("google.com")
                        .description("Google main website")
                        .active(true)
                        .port(443)
                        .checkIntervalMinutes(1440)
                        .webhookUrl("")
                        .notificationEmail("admin@example.com")
                        .build(),
                
                Domain.builder()
                        .name("github.com")
                        .description("GitHub platform")
                        .active(true)
                        .port(443)
                        .checkIntervalMinutes(1440)
                        .webhookUrl("")
                        .notificationEmail("admin@example.com")
                        .build(),
                
                Domain.builder()
                        .name("stackoverflow.com")
                        .description("Stack Overflow Q&A platform")
                        .active(true)
                        .port(443)
                        .checkIntervalMinutes(1440)
                        .webhookUrl("")
                        .notificationEmail("admin@example.com")
                        .build(),
                
                Domain.builder()
                        .name("example.com")
                        .description("Example domain for testing")
                        .active(true)
                        .port(443)
                        .checkIntervalMinutes(720) // Check every 12 hours
                        .webhookUrl("")
                        .notificationEmail("admin@example.com")
                        .build(),
                
                Domain.builder()
                        .name("httpbin.org")
                        .description("HTTP testing service")
                        .active(true)
                        .port(443)
                        .checkIntervalMinutes(1440)
                        .webhookUrl("")
                        .notificationEmail("admin@example.com")
                        .build()
        );
        
        domainRepository.saveAll(sampleDomains);
        log.info("Loaded {} sample domains", sampleDomains.size());
    }
} 