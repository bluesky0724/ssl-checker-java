package org.avengers.boilerplate.service;

import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.domain.entity.SslCertificate;
import org.avengers.boilerplate.repository.SslCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SslCertificateCheckerTest {
    
    @Mock
    private SslCertificateRepository sslCertificateRepository;
    
//    @Mock
//    private NotificationService notificationService;
    
    @Mock
    private Executor asyncExecutor;
    
    @InjectMocks
    private SslCertificateChecker sslCertificateChecker;
    
    private Domain testDomain;
    
    @BeforeEach
    void setUp() {
        testDomain = Domain.builder()
                .id(1L)
                .name("example.com")
                .port(443)
                .active(true)
                .build();
        
        // Set configuration values
        ReflectionTestUtils.setField(sslCertificateChecker, "criticalThreshold", 7);
        ReflectionTestUtils.setField(sslCertificateChecker, "warningThreshold", 30);
        ReflectionTestUtils.setField(sslCertificateChecker, "infoThreshold", 90);
        ReflectionTestUtils.setField(sslCertificateChecker, "connectTimeout", 10000);
        ReflectionTestUtils.setField(sslCertificateChecker, "readTimeout", 10000);
        ReflectionTestUtils.setField(sslCertificateChecker, "maxRetries", 3);
    }
    
    @Test
    void testCheckCertificate_ValidDomain() {
        // Given
        when(sslCertificateRepository.save(any(SslCertificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SslCertificate result = sslCertificateChecker.checkCertificate(testDomain);
        
        // Then
        assertNotNull(result);
        assertEquals(testDomain, result.getDomain());
        assertNotNull(result.getCheckDate());
        assertTrue(result.getResponseTimeMs() > 0);
        
        // Verify that the certificate was saved
        verify(sslCertificateRepository).save(any(SslCertificate.class));
    }
    
    @Test
    void testCheckCertificate_InvalidDomain() {
        // Given
        Domain invalidDomain = Domain.builder()
                .id(2L)
                .name("invalid-domain-that-does-not-exist.com")
                .port(443)
                .active(true)
                .build();
        
        when(sslCertificateRepository.save(any(SslCertificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SslCertificate result = sslCertificateChecker.checkCertificate(invalidDomain);
        
        // Then
        assertNotNull(result);
        assertEquals(invalidDomain, result.getDomain());
        assertEquals(SslCertificate.CertificateStatus.ERROR, result.getStatus());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getResponseTimeMs() > 0);
        
        // Verify that the certificate was saved
        verify(sslCertificateRepository).save(any(SslCertificate.class));
    }
    
    @Test
    void testCheckCertificate_NonStandardPort() {
        // Given
        Domain customPortDomain = Domain.builder()
                .id(3L)
                .name("example.com")
                .port(8443)
                .active(true)
                .build();
        
        when(sslCertificateRepository.save(any(SslCertificate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        SslCertificate result = sslCertificateChecker.checkCertificate(customPortDomain);
        
        // Then
        assertNotNull(result);
        assertEquals(customPortDomain, result.getDomain());
        assertTrue(result.getResponseTimeMs() > 0);
        
        // Verify that the certificate was saved
        verify(sslCertificateRepository).save(any(SslCertificate.class));
    }
    
//    @Test
//    void testCheckCertificateAsync() {
//        // Given
//        when(asyncExecutor.execute(any(Runnable.class)))
//                .thenAnswer(invocation -> {
//                    Runnable runnable = invocation.getArgument(0);
//                    runnable.run();
//                    return null;
//                });
//
//        when(sslCertificateRepository.save(any(SslCertificate.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        var future = sslCertificateChecker.checkCertificateAsync(testDomain);
//
//        // Then
//        assertNotNull(future);
//        assertTrue(future.isDone());
//
//        SslCertificate result = future.join();
//        assertNotNull(result);
//        assertEquals(testDomain, result.getDomain());
//
//        verify(asyncExecutor).execute(any(Runnable.class));
//        verify(sslCertificateRepository).save(any(SslCertificate.class));
//    }
//
//    @Test
//    void testCheckMultipleCertificates() {
//        // Given
//        Domain domain1 = Domain.builder().id(1L).name("example1.com").port(443).active(true).build();
//        Domain domain2 = Domain.builder().id(2L).name("example2.com").port(443).active(true).build();
//
//        when(sslCertificateRepository.save(any(SslCertificate.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        var results = sslCertificateChecker.checkMultipleCertificates(List.of(domain1, domain2));
//
//        // Then
//        assertNotNull(results);
//        assertEquals(2, results.size());
//
//        verify(sslCertificateRepository, times(2)).save(any(SslCertificate.class));
//    }
//
//    @Test
//    void testCheckMultipleCertificates_EmptyList() {
//        // When
//        var results = sslCertificateChecker.checkMultipleCertificates(List.of());
//
//        // Then
//        assertNotNull(results);
//        assertTrue(results.isEmpty());
//
//        verify(sslCertificateRepository, never()).save(any(SslCertificate.class));
//    }
//
//    @Test
//    void testCheckMultipleCertificates_Exception() {
//        // Given
//        Domain domain = Domain.builder().id(1L).name("example.com").port(443).active(true).build();
//
//        when(sslCertificateRepository.save(any(SslCertificate.class)))
//                .thenThrow(new RuntimeException("Database error"));
//
//        // When & Then
//        assertThrows(RuntimeException.class, () -> {
//            sslCertificateChecker.checkMultipleCertificates(List.of(domain));
//        });
//    }
} 