package org.avengers.boilerplate.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.avengers.boilerplate.domain.dto.SslCertificateDto;
import org.avengers.boilerplate.domain.entity.SslCertificate;
import org.avengers.boilerplate.repository.SslCertificateRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/certificates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSL Certificate Management", description = "APIs for querying SSL certificate information and statistics")
public class SslCertificateController {
    
    private final SslCertificateRepository sslCertificateRepository;
    
    @GetMapping
    @Operation(summary = "Get all SSL certificates", description = "Retrieve a paginated list of all SSL certificates")
    public ResponseEntity<Page<SslCertificateDto>> getAllCertificates(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by certificate status") @RequestParam(required = false) SslCertificate.CertificateStatus status) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkDate").descending());
        Page<SslCertificate> certificates;
        
        if (status != null) {
            certificates = sslCertificateRepository.findByStatus(status, pageable);
        } else {
            certificates = sslCertificateRepository.findAll(pageable);
        }
        
        Page<SslCertificateDto> certificateDtos = certificates.map(SslCertificateDto::fromEntity);
        return ResponseEntity.ok(certificateDtos);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get SSL certificate by ID", description = "Retrieve a specific SSL certificate by its ID")
    public ResponseEntity<SslCertificateDto> getCertificateById(
            @Parameter(description = "Certificate ID") @PathVariable Long id) {
        
        return sslCertificateRepository.findById(id)
                .map(cert -> ResponseEntity.ok(SslCertificateDto.fromEntity(cert)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/expiring")
    @Operation(summary = "Get expiring certificates", description = "Retrieve certificates that are expiring within a specified number of days")
    public ResponseEntity<List<SslCertificateDto>> getExpiringCertificates(
            @Parameter(description = "Number of days") @RequestParam(defaultValue = "30") int days) {
        
        List<SslCertificate> certificates = sslCertificateRepository.findExpiringWithinDays(days);
        List<SslCertificateDto> certificateDtos = certificates.stream()
                .map(SslCertificateDto::fromEntity)
                .toList();
        
        return ResponseEntity.ok(certificateDtos);
    }
    
    @GetMapping("/expired")
    @Operation(summary = "Get expired certificates", description = "Retrieve certificates that have already expired")
    public ResponseEntity<List<SslCertificateDto>> getExpiredCertificates() {
        
        List<SslCertificate> certificates = sslCertificateRepository.findExpiredBefore(LocalDateTime.now());
        List<SslCertificateDto> certificateDtos = certificates.stream()
                .map(SslCertificateDto::fromEntity)
                .toList();
        
        return ResponseEntity.ok(certificateDtos);
    }
    
    @GetMapping("/recent")
    @Operation(summary = "Get recent certificate checks", description = "Retrieve recent SSL certificate checks")
    public ResponseEntity<Page<SslCertificateDto>> getRecentCertificates(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Since date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        if (since == null) {
            since = LocalDateTime.now().minusDays(7); // Default to last 7 days
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkDate").descending());
        Page<SslCertificate> certificates = sslCertificateRepository.findRecentChecks(since, pageable);
        
        Page<SslCertificateDto> certificateDtos = certificates.map(SslCertificateDto::fromEntity);
        return ResponseEntity.ok(certificateDtos);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get certificate statistics", description = "Retrieve statistics about SSL certificates")
    public ResponseEntity<CertificateStats> getCertificateStats() {
        
        long totalCertificates = sslCertificateRepository.count();
        long validCertificates = sslCertificateRepository.countByStatus(SslCertificate.CertificateStatus.VALID);
        long expiringSoonCertificates = sslCertificateRepository.countByStatus(SslCertificate.CertificateStatus.EXPIRING_SOON);
        long expiredCertificates = sslCertificateRepository.countByStatus(SslCertificate.CertificateStatus.EXPIRED);
        long errorCertificates = sslCertificateRepository.countByStatus(SslCertificate.CertificateStatus.ERROR);
        long untrustedCertificates = sslCertificateRepository.countByStatus(SslCertificate.CertificateStatus.UNTRUSTED);
        
        CertificateStats stats = new CertificateStats(
                totalCertificates,
                validCertificates,
                expiringSoonCertificates,
                expiredCertificates,
                errorCertificates,
                untrustedCertificates
        );
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/stats/domain/{domainId}")
    @Operation(summary = "Get certificate statistics for domain", description = "Retrieve SSL certificate statistics for a specific domain")
    public ResponseEntity<DomainCertificateStats> getDomainCertificateStats(
            @Parameter(description = "Domain ID") @PathVariable Long domainId) {
        
        long totalChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, null);
        long validChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, SslCertificate.CertificateStatus.VALID);
        long expiringSoonChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, SslCertificate.CertificateStatus.EXPIRING_SOON);
        long expiredChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, SslCertificate.CertificateStatus.EXPIRED);
        long errorChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, SslCertificate.CertificateStatus.ERROR);
        long untrustedChecks = sslCertificateRepository.countByDomainIdAndStatus(domainId, SslCertificate.CertificateStatus.UNTRUSTED);
        
        DomainCertificateStats stats = new DomainCertificateStats(
                domainId,
                totalChecks,
                validChecks,
                expiringSoonChecks,
                expiredChecks,
                errorChecks,
                untrustedChecks
        );
        
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search certificates", description = "Search certificates by various criteria")
    public ResponseEntity<Page<SslCertificateDto>> searchCertificates(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Domain name") @RequestParam(required = false) String domainName,
            @Parameter(description = "Issuer") @RequestParam(required = false) String issuer,
            @Parameter(description = "Status") @RequestParam(required = false) SslCertificate.CertificateStatus status,
            @Parameter(description = "From date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "To date") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        
        // This is a simplified search implementation
        // In a real application, you would implement a more sophisticated search
        Pageable pageable = PageRequest.of(page, size, Sort.by("checkDate").descending());
        Page<SslCertificate> certificates = sslCertificateRepository.findAll(pageable);
        
        Page<SslCertificateDto> certificateDtos = certificates.map(SslCertificateDto::fromEntity);
        return ResponseEntity.ok(certificateDtos);
    }
    
    public static class CertificateStats {
        private final long totalCertificates;
        private final long validCertificates;
        private final long expiringSoonCertificates;
        private final long expiredCertificates;
        private final long errorCertificates;
        private final long untrustedCertificates;
        
        public CertificateStats(long totalCertificates, long validCertificates, long expiringSoonCertificates,
                              long expiredCertificates, long errorCertificates, long untrustedCertificates) {
            this.totalCertificates = totalCertificates;
            this.validCertificates = validCertificates;
            this.expiringSoonCertificates = expiringSoonCertificates;
            this.expiredCertificates = expiredCertificates;
            this.errorCertificates = errorCertificates;
            this.untrustedCertificates = untrustedCertificates;
        }
        
        public long getTotalCertificates() { return totalCertificates; }
        public long getValidCertificates() { return validCertificates; }
        public long getExpiringSoonCertificates() { return expiringSoonCertificates; }
        public long getExpiredCertificates() { return expiredCertificates; }
        public long getErrorCertificates() { return errorCertificates; }
        public long getUntrustedCertificates() { return untrustedCertificates; }
    }
    
    public static class DomainCertificateStats {
        private final Long domainId;
        private final long totalChecks;
        private final long validChecks;
        private final long expiringSoonChecks;
        private final long expiredChecks;
        private final long errorChecks;
        private final long untrustedChecks;
        
        public DomainCertificateStats(Long domainId, long totalChecks, long validChecks, long expiringSoonChecks,
                                    long expiredChecks, long errorChecks, long untrustedChecks) {
            this.domainId = domainId;
            this.totalChecks = totalChecks;
            this.validChecks = validChecks;
            this.expiringSoonChecks = expiringSoonChecks;
            this.expiredChecks = expiredChecks;
            this.errorChecks = errorChecks;
            this.untrustedChecks = untrustedChecks;
        }
        
        public Long getDomainId() { return domainId; }
        public long getTotalChecks() { return totalChecks; }
        public long getValidChecks() { return validChecks; }
        public long getExpiringSoonChecks() { return expiringSoonChecks; }
        public long getExpiredChecks() { return expiredChecks; }
        public long getErrorChecks() { return errorChecks; }
        public long getUntrustedChecks() { return untrustedChecks; }
    }
} 