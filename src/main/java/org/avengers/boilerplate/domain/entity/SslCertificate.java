package org.avengers.boilerplate.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ssl_certificates", indexes = {
    @Index(name = "idx_domain_id", columnList = "domain_id"),
    @Index(name = "idx_check_date", columnList = "check_date"),
    @Index(name = "idx_expiry_date", columnList = "expiry_date"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SslCertificate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;
    
    @Column(name = "subject", length = 1000)
    private String subject;
    
    @Column(name = "issuer", length = 1000)
    private String issuer;
    
    @Column(name = "serial_number", length = 100)
    private String serialNumber;
    
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(name = "days_until_expiry")
    private Integer daysUntilExpiry;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CertificateStatus status;
    
    @Column(name = "error_message", length = 2000)
    private String errorMessage;
    
    @Column(name = "check_date", nullable = false)
    @CreationTimestamp
    private LocalDateTime checkDate;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "certificate_chain", columnDefinition = "TEXT")
    private String certificateChain;
    
    @Column(name = "fingerprint_sha256", length = 64)
    private String fingerprintSha256;
    
    @Column(name = "signature_algorithm", length = 100)
    private String signatureAlgorithm;
    
    @Column(name = "key_size")
    private Integer keySize;
    
    @Column(name = "subject_alternative_names", columnDefinition = "TEXT")
    private String subjectAlternativeNames;
    
    public enum CertificateStatus {
        VALID,
        EXPIRING_SOON,
        EXPIRED,
        ERROR,
        UNTRUSTED
    }
} 