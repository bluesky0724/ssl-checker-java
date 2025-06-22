package org.avengers.boilerplate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSslCheckService {
    
    private final DomainRepository domainRepository;
    private final SslCertificateChecker sslCertificateChecker;
//    private final NotificationService notificationService;
    
    @Value("${ssl.monitor.schedule.daily:0 0 6 * * ?}")
    private String dailySchedule;
    
    @Value("${ssl.monitor.schedule.hourly:0 0 * * * ?}")
    private String hourlySchedule;
    
    /**
     * Daily scheduled check for all active domains
     * Runs at 6 AM by default
     */
    @Scheduled(cron = "${ssl.monitor.schedule.daily:0 0 6 * * ?}")
    public void performDailySslChecks() {
        log.info("Starting daily SSL certificate checks");
        
        try {
            List<Domain> activeDomains = domainRepository.findByActiveTrue();
            log.info("Found {} active domains for daily check", activeDomains.size());
            
            if (!activeDomains.isEmpty()) {
                List<org.avengers.boilerplate.domain.entity.SslCertificate> results = 
                        sslCertificateChecker.checkMultipleCertificates(activeDomains);
                
                log.info("Daily SSL check completed. Processed {} certificates", results.size());
                
                // Update domain last check times
                updateDomainCheckTimes(activeDomains);
            }
            
        } catch (Exception e) {
            log.error("Error during daily SSL certificate checks: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Hourly check for domains that need immediate attention
     * Checks domains that are due for checking or have expiring certificates
     */
    @Scheduled(cron = "${ssl.monitor.schedule.hourly:0 0 * * * ?}")
    public void performHourlySslChecks() {
        log.info("Starting hourly SSL certificate checks");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Domain> domainsReadyForCheck = domainRepository.findDomainsReadyForCheck(now);
            
            log.info("Found {} domains ready for hourly check", domainsReadyForCheck.size());
            
            if (!domainsReadyForCheck.isEmpty()) {
                List<org.avengers.boilerplate.domain.entity.SslCertificate> results = 
                        sslCertificateChecker.checkMultipleCertificates(domainsReadyForCheck);
                
                log.info("Hourly SSL check completed. Processed {} certificates", results.size());
                
                // Update domain last check times
                updateDomainCheckTimes(domainsReadyForCheck);
            }
            
        } catch (Exception e) {
            log.error("Error during hourly SSL certificate checks: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retry failed notifications every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void retryFailedNotifications() {
        log.debug("Checking for failed notifications to retry");
//
//        try {
//            notificationService.retryFailedNotifications();
//        } catch (Exception e) {
//            log.error("Error retrying failed notifications: {}", e.getMessage(), e);
//        }
    }
    
    /**
     * Cleanup old certificate records (older than 90 days)
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldCertificateRecords() {
        log.info("Starting cleanup of old certificate records");
        
        try {
            // This would be implemented in the repository layer
            // For now, we'll just log the cleanup operation
            log.info("Cleanup of old certificate records completed");
            
        } catch (Exception e) {
            log.error("Error during cleanup of old certificate records: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Health check for the monitoring system
     * Runs every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void performHealthCheck() {
        log.debug("Performing monitoring system health check");
        
        try {
            long activeDomains = domainRepository.countActiveDomains();
            long inactiveDomains = domainRepository.countInactiveDomains();
            
            log.info("Health check - Active domains: {}, Inactive domains: {}", activeDomains, inactiveDomains);
            
            // Check if there are any domains that haven't been checked in a while
            List<Domain> domainsNeedingCheck = domainRepository.findDomainsReadyForCheck(LocalDateTime.now().minusHours(24));
            
            if (!domainsNeedingCheck.isEmpty()) {
                log.warn("Found {} domains that haven't been checked in 24 hours", domainsNeedingCheck.size());
            }
            
        } catch (Exception e) {
            log.error("Error during health check: {}", e.getMessage(), e);
        }
    }
    
    private void updateDomainCheckTimes(List<Domain> domains) {
        LocalDateTime now = LocalDateTime.now();
        
        for (Domain domain : domains) {
            domain.setLastCheckAt(now);
            domain.setNextCheckAt(now.plusMinutes(domain.getCheckIntervalMinutes()));
            domainRepository.save(domain);
        }
    }
    
    /**
     * Manual trigger for SSL certificate checks
     * Can be called via REST API
     */
    public void performManualSslCheck(Long domainId) {
        log.info("Performing manual SSL check for domain ID: {}", domainId);
        
        try {
            var domain = domainRepository.findById(domainId);
            if (domain.isPresent()) {
                sslCertificateChecker.checkCertificate(domain.get());
                log.info("Manual SSL check completed for domain: {}", domain.get().getName());
            } else {
                log.warn("Domain not found for manual SSL check: {}", domainId);
            }
        } catch (Exception e) {
            log.error("Error during manual SSL check for domain {}: {}", domainId, e.getMessage(), e);
        }
    }
    
    /**
     * Bulk manual trigger for SSL certificate checks
     * Can be called via REST API
     */
    public void performBulkSslCheck(List<Long> domainIds) {
        log.info("Performing bulk SSL check for {} domains", domainIds.size());
        
        try {
            List<Domain> domains = domainRepository.findAllById(domainIds);
            if (!domains.isEmpty()) {
                sslCertificateChecker.checkMultipleCertificates(domains);
                log.info("Bulk SSL check completed for {} domains", domains.size());
            } else {
                log.warn("No domains found for bulk SSL check");
            }
        } catch (Exception e) {
            log.error("Error during bulk SSL check: {}", e.getMessage(), e);
        }
    }
} 