package org.avengers.boilerplate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.domain.entity.SslCertificate;
import org.avengers.boilerplate.repository.SslCertificateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class SslCertificateChecker {
    
    private final SslCertificateRepository sslCertificateRepository;
//    private final NotificationService notificationService;
    
    @Value("${ssl.monitor.thresholds.critical:7}")
    private int criticalThreshold;
    
    @Value("${ssl.monitor.thresholds.warning:30}")
    private int warningThreshold;
    
    @Value("${ssl.monitor.thresholds.info:90}")
    private int infoThreshold;
    
    @Value("${ssl.monitor.http.connect-timeout:10000}")
    private int connectTimeout;
    
    @Value("${ssl.monitor.http.read-timeout:10000}")
    private int readTimeout;
    
    @Value("${ssl.monitor.http.max-retries:3}")
    private int maxRetries;
    
    private final Executor asyncExecutor;
    
    public CompletableFuture<SslCertificate> checkCertificateAsync(Domain domain) {
        return CompletableFuture.supplyAsync(() -> checkCertificate(domain), asyncExecutor);
    }
    
    public SslCertificate checkCertificate(Domain domain) {
        log.info("Checking SSL certificate for domain: {}", domain.getName());
        
        long startTime = System.currentTimeMillis();
        SslCertificate.CertificateStatus status = SslCertificate.CertificateStatus.ERROR;
        String errorMessage = null;
        String subject = null;
        String issuer = null;
        String serialNumber = null;
        LocalDateTime validFrom = null;
        LocalDateTime expiryDate = null;
        Integer daysUntilExpiry = null;
        String fingerprintSha256 = null;
        String signatureAlgorithm = null;
        Integer keySize = null;
        String subjectAlternativeNames = null;
        
        try {
            // Create SSL context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            
            // Create socket factory
            SSLSocketFactory factory = sslContext.getSocketFactory();
            
            // Connect to the domain
            try (SSLSocket socket = (SSLSocket) factory.createSocket(domain.getName(), domain.getPort())) {
                socket.setSoTimeout(readTimeout);
                
                // Start SSL handshake
                socket.startHandshake();
                
                // Get the session
                SSLSession session = socket.getSession();
                Certificate[] certificates = session.getPeerCertificates();
                
                if (certificates.length > 0 && certificates[0] instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate) certificates[0];
                    
                    // Extract certificate information
                    subject = cert.getSubjectX500Principal().getName();
                    issuer = cert.getIssuerX500Principal().getName();
                    serialNumber = cert.getSerialNumber().toString();
                    validFrom = cert.getNotBefore().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    expiryDate = cert.getNotAfter().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
                    daysUntilExpiry = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), expiryDate);
                    fingerprintSha256 = calculateFingerprint(cert);
                    signatureAlgorithm = cert.getSigAlgName();
                    keySize = cert.getPublicKey().getEncoded().length * 8;
                    
                    // Get subject alternative names
                    subjectAlternativeNames = extractSubjectAlternativeNames(cert);
                    
                    // Determine status based on expiry
                    if (daysUntilExpiry < 0) {
                        status = SslCertificate.CertificateStatus.EXPIRED;
                    } else if (daysUntilExpiry <= criticalThreshold) {
                        status = SslCertificate.CertificateStatus.EXPIRING_SOON;
                    } else {
                        status = SslCertificate.CertificateStatus.VALID;
                    }
                    
                    log.info("SSL certificate check completed for {}: status={}, daysUntilExpiry={}", 
                            domain.getName(), status, daysUntilExpiry);
                } else {
                    errorMessage = "No valid X509 certificate found";
                    log.warn("No valid X509 certificate found for domain: {}", domain.getName());
                }
                
            } catch (SSLHandshakeException e) {
                errorMessage = "SSL handshake failed: " + e.getMessage();
                log.warn("SSL handshake failed for domain {}: {}", domain.getName(), e.getMessage());
            } catch (IOException e) {
                errorMessage = "Connection failed: " + e.getMessage();
                log.warn("Connection failed for domain {}: {}", domain.getName(), e.getMessage());
            }
            
        } catch (Exception e) {
            errorMessage = "Unexpected error: " + e.getMessage();
            log.error("Unexpected error checking SSL certificate for domain {}: {}", domain.getName(), e.getMessage(), e);
        }
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Create and save the certificate record
        SslCertificate certificate = SslCertificate.builder()
                .domain(domain)
                .subject(subject)
                .issuer(issuer)
                .serialNumber(serialNumber)
                .validFrom(validFrom)
                .expiryDate(expiryDate)
                .daysUntilExpiry(daysUntilExpiry)
                .status(status)
                .errorMessage(errorMessage)
                .responseTimeMs(responseTime)
                .fingerprintSha256(fingerprintSha256)
                .signatureAlgorithm(signatureAlgorithm)
                .keySize(keySize)
                .subjectAlternativeNames(subjectAlternativeNames)
                .build();
        
        SslCertificate savedCertificate = sslCertificateRepository.save(certificate);
        
        // Send notifications if needed
//        if (status == SslCertificate.CertificateStatus.EXPIRING_SOON ||
//            status == SslCertificate.CertificateStatus.EXPIRED) {
//            notificationService.sendExpiryNotification(domain, savedCertificate);
//        }
        
        return savedCertificate;
    }
    
    private String calculateFingerprint(X509Certificate cert) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to calculate certificate fingerprint: {}", e.getMessage());
            return null;
        }
    }
    
    private String extractSubjectAlternativeNames(X509Certificate cert) {
        try {
            java.util.Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans != null) {
                List<String> sanList = new ArrayList<>();
                for (List<?> san : sans) {
                    if (san.size() >= 2) {
                        sanList.add(san.get(1).toString());
                    }
                }
                return String.join(",", sanList);
            }
        } catch (Exception e) {
            log.warn("Failed to extract subject alternative names: {}", e.getMessage());
        }
        return null;
    }
    
    public List<SslCertificate> checkMultipleCertificates(List<Domain> domains) {
        log.info("Starting bulk SSL certificate check for {} domains", domains.size());
        
        List<CompletableFuture<SslCertificate>> futures = domains.stream()
                .map(this::checkCertificateAsync)
                .toList();
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(30, TimeUnit.MINUTES); // 30 minute timeout for bulk operations
            
            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
                    
        } catch (Exception e) {
            log.error("Bulk SSL certificate check failed: {}", e.getMessage(), e);
            throw new RuntimeException("Bulk SSL certificate check failed", e);
        }
    }
} 