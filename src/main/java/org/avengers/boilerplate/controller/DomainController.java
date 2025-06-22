package org.avengers.boilerplate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.avengers.boilerplate.domain.dto.DomainDto;
import org.avengers.boilerplate.domain.dto.SslCertificateDto;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.repository.DomainRepository;
import org.avengers.boilerplate.repository.SslCertificateRepository;
import org.avengers.boilerplate.service.ScheduledSslCheckService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/domains")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Domain Management", description = "APIs for managing domains and SSL certificate monitoring")
public class DomainController {
    
    private final DomainRepository domainRepository;
    private final SslCertificateRepository sslCertificateRepository;
    private final ScheduledSslCheckService scheduledSslCheckService;
    
    @GetMapping
    @Operation(summary = "Get all domains", description = "Retrieve a paginated list of all domains")
    public ResponseEntity<Page<DomainDto>> getAllDomains(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Search term for domain name") @RequestParam(required = false) String search) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Domain> domains;
        
        if (search != null && !search.trim().isEmpty()) {
            domains = domainRepository.findActiveDomainsByNameContaining(search.trim(), pageable);
        } else {
            domains = domainRepository.findAll(pageable);
        }
        
        Page<DomainDto> domainDtos = domains.map(this::convertToDto);
        return ResponseEntity.ok(domainDtos);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get domain by ID", description = "Retrieve a specific domain by its ID")
    public ResponseEntity<DomainDto> getDomainById(
            @Parameter(description = "Domain ID") @PathVariable Long id) {
        
        Optional<Domain> domain = domainRepository.findById(id);
        return domain.map(d -> ResponseEntity.ok(convertToDto(d)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @Operation(summary = "Create new domain", description = "Create a new domain for SSL monitoring")
    public ResponseEntity<DomainDto> createDomain(
            @Parameter(description = "Domain information") @Valid @RequestBody DomainDto domainDto) {
        
        if (domainRepository.existsByName(domainDto.getName())) {
            return ResponseEntity.badRequest().build();
        }
        
        Domain domain = Domain.builder()
                .name(domainDto.getName())
                .description(domainDto.getDescription())
                .active(domainDto.getActive() != null ? domainDto.getActive() : true)
                .port(domainDto.getPort() != null ? domainDto.getPort() : 443)
                .checkIntervalMinutes(domainDto.getCheckIntervalMinutes() != null ? domainDto.getCheckIntervalMinutes() : 1440)
                .webhookUrl(domainDto.getWebhookUrl())
                .notificationEmail(domainDto.getNotificationEmail())
                .build();
        
        Domain savedDomain = domainRepository.save(domain);
        log.info("Created new domain: {}", savedDomain.getName());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedDomain));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update domain", description = "Update an existing domain")
    public ResponseEntity<DomainDto> updateDomain(
            @Parameter(description = "Domain ID") @PathVariable Long id,
            @Parameter(description = "Updated domain information") @Valid @RequestBody DomainDto domainDto) {
        
        Optional<Domain> existingDomain = domainRepository.findById(id);
        if (existingDomain.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Domain domain = existingDomain.get();
        domain.setName(domainDto.getName());
        domain.setDescription(domainDto.getDescription());
        domain.setActive(domainDto.getActive());
        domain.setPort(domainDto.getPort());
        domain.setCheckIntervalMinutes(domainDto.getCheckIntervalMinutes());
        domain.setWebhookUrl(domainDto.getWebhookUrl());
        domain.setNotificationEmail(domainDto.getNotificationEmail());
        
        Domain updatedDomain = domainRepository.save(domain);
        log.info("Updated domain: {}", updatedDomain.getName());
        
        return ResponseEntity.ok(convertToDto(updatedDomain));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete domain", description = "Delete a domain (soft delete by setting active to false)")
    public ResponseEntity<Void> deleteDomain(
            @Parameter(description = "Domain ID") @PathVariable Long id) {
        
        Optional<Domain> domain = domainRepository.findById(id);
        if (domain.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Domain domainToDelete = domain.get();
        domainToDelete.setActive(false);
        domainRepository.save(domainToDelete);
        
        log.info("Deactivated domain: {}", domainToDelete.getName());
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/certificates")
    @Operation(summary = "Get SSL certificates for domain", description = "Retrieve SSL certificate history for a specific domain")
    public ResponseEntity<Page<SslCertificateDto>> getDomainCertificates(
            @Parameter(description = "Domain ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        
        if (!domainRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkDate").descending());
        Page<org.avengers.boilerplate.domain.entity.SslCertificate> certificates = 
                sslCertificateRepository.findByDomainId(id, pageable);
        
        Page<SslCertificateDto> certificateDtos = certificates.map(SslCertificateDto::fromEntity);
        return ResponseEntity.ok(certificateDtos);
    }
    
    @GetMapping("/{id}/certificates/latest")
    @Operation(summary = "Get latest SSL certificate", description = "Retrieve the most recent SSL certificate for a domain")
    public ResponseEntity<SslCertificateDto> getLatestCertificate(
            @Parameter(description = "Domain ID") @PathVariable Long id) {
        
        Optional<org.avengers.boilerplate.domain.entity.SslCertificate> certificate = 
                sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(id);
        
        return certificate.map(cert -> ResponseEntity.ok(SslCertificateDto.fromEntity(cert)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{id}/check")
    @Operation(summary = "Trigger manual SSL check", description = "Manually trigger an SSL certificate check for a domain")
    public ResponseEntity<Void> triggerSslCheck(
            @Parameter(description = "Domain ID") @PathVariable Long id) {
        
        if (!domainRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        scheduledSslCheckService.performManualSslCheck(id);
        return ResponseEntity.accepted().build();
    }
    
    @PostMapping("/bulk-check")
    @Operation(summary = "Trigger bulk SSL check", description = "Manually trigger SSL certificate checks for multiple domains")
    public ResponseEntity<Void> triggerBulkSslCheck(
            @Parameter(description = "List of domain IDs") @RequestBody List<Long> domainIds) {
        
        scheduledSslCheckService.performBulkSslCheck(domainIds);
        return ResponseEntity.accepted().build();
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get domain statistics", description = "Retrieve statistics about monitored domains")
    public ResponseEntity<DomainStats> getDomainStats() {
        long activeDomains = domainRepository.countActiveDomains();
        long inactiveDomains = domainRepository.countInactiveDomains();
        long totalDomains = activeDomains + inactiveDomains;
        
        DomainStats stats = new DomainStats(totalDomains, activeDomains, inactiveDomains);
        return ResponseEntity.ok(stats);
    }
    
    private DomainDto convertToDto(Domain domain) {
        // Get latest certificate and statistics
        Optional<org.avengers.boilerplate.domain.entity.SslCertificate> latestCert = 
                sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(domain.getId());
        
        long totalChecks = sslCertificateRepository.countByDomainIdAndStatus(domain.getId(), null);
        long failedChecks = sslCertificateRepository.countByDomainIdAndStatus(domain.getId(), 
                org.avengers.boilerplate.domain.entity.SslCertificate.CertificateStatus.ERROR);
        
        return DomainDto.builder()
                .id(domain.getId())
                .name(domain.getName())
                .description(domain.getDescription())
                .active(domain.getActive())
                .port(domain.getPort())
                .checkIntervalMinutes(domain.getCheckIntervalMinutes())
                .webhookUrl(domain.getWebhookUrl())
                .notificationEmail(domain.getNotificationEmail())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .lastCheckAt(domain.getLastCheckAt())
                .nextCheckAt(domain.getNextCheckAt())
                .latestCertificate(latestCert.map(SslCertificateDto::fromEntity).orElse(null))
                .totalChecks(totalChecks)
                .failedChecks(failedChecks)
                .build();
    }
    
    public static class DomainStats {
        private final long totalDomains;
        private final long activeDomains;
        private final long inactiveDomains;
        
        public DomainStats(long totalDomains, long activeDomains, long inactiveDomains) {
            this.totalDomains = totalDomains;
            this.activeDomains = activeDomains;
            this.inactiveDomains = inactiveDomains;
        }
        
        public long getTotalDomains() { return totalDomains; }
        public long getActiveDomains() { return activeDomains; }
        public long getInactiveDomains() { return inactiveDomains; }
    }
} 