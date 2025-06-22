package org.avengers.boilerplate.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.avengers.boilerplate.domain.entity.SslCertificate;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SslCertificateDto {
    
    private Long id;
    
    private Long domainId;
    
    private String domainName;
    
    private String subject;
    
    private String issuer;
    
    private String serialNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiryDate;
    
    private Integer daysUntilExpiry;
    
    private CertificateStatus status;
    
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkDate;
    
    private Long responseTimeMs;
    
    private String fingerprintSha256;
    
    private String signatureAlgorithm;
    
    private Integer keySize;
    
    private String subjectAlternativeNames;
    
    public enum CertificateStatus {
        VALID,
        EXPIRING_SOON,
        EXPIRED,
        ERROR,
        UNTRUSTED
    }
    
    public static SslCertificateDto fromEntity(SslCertificate entity) {
        return SslCertificateDto.builder()
                .id(entity.getId())
                .domainId(entity.getDomain().getId())
                .domainName(entity.getDomain().getName())
                .subject(entity.getSubject())
                .issuer(entity.getIssuer())
                .serialNumber(entity.getSerialNumber())
                .validFrom(entity.getValidFrom())
                .expiryDate(entity.getExpiryDate())
                .daysUntilExpiry(entity.getDaysUntilExpiry())
//                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .checkDate(entity.getCheckDate())
                .responseTimeMs(entity.getResponseTimeMs())
                .fingerprintSha256(entity.getFingerprintSha256())
                .signatureAlgorithm(entity.getSignatureAlgorithm())
                .keySize(entity.getKeySize())
                .subjectAlternativeNames(entity.getSubjectAlternativeNames())
                .build();
    }
} 