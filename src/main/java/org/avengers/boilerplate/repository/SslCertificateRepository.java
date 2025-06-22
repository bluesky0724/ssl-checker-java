package org.avengers.boilerplate.repository;

import org.avengers.boilerplate.domain.entity.SslCertificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SslCertificateRepository extends JpaRepository<SslCertificate, Long> {
    
    List<SslCertificate> findByDomainIdOrderByCheckDateDesc(Long domainId);
    
    Optional<SslCertificate> findFirstByDomainIdOrderByCheckDateDesc(Long domainId);
    
    List<SslCertificate> findByDomainIdAndCheckDateBetweenOrderByCheckDateDesc(
            Long domainId, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.domain.id = :domainId ORDER BY sc.checkDate DESC")
    Page<SslCertificate> findByDomainId(@Param("domainId") Long domainId, Pageable pageable);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.status = :status ORDER BY sc.checkDate DESC")
    Page<SslCertificate> findByStatus(@Param("status") SslCertificate.CertificateStatus status, Pageable pageable);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.expiryDate BETWEEN :startDate AND :endDate ORDER BY sc.expiryDate ASC")
    List<SslCertificate> findExpiringBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.expiryDate <= :date ORDER BY sc.expiryDate ASC")
    List<SslCertificate> findExpiredBefore(@Param("date") LocalDateTime date);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.daysUntilExpiry <= :days ORDER BY sc.daysUntilExpiry ASC")
    List<SslCertificate> findExpiringWithinDays(@Param("days") Integer days);
    
    @Query("SELECT COUNT(sc) FROM SslCertificate sc WHERE sc.status = :status")
    long countByStatus(@Param("status") SslCertificate.CertificateStatus status);
    
    @Query("SELECT COUNT(sc) FROM SslCertificate sc WHERE sc.domain.id = :domainId AND sc.status = :status")
    long countByDomainIdAndStatus(@Param("domainId") Long domainId, @Param("status") SslCertificate.CertificateStatus status);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.domain.id = :domainId AND sc.status = 'ERROR' ORDER BY sc.checkDate DESC")
    List<SslCertificate> findErrorsByDomainId(@Param("domainId") Long domainId);
    
    @Query("SELECT sc FROM SslCertificate sc WHERE sc.checkDate >= :since ORDER BY sc.checkDate DESC")
    Page<SslCertificate> findRecentChecks(@Param("since") LocalDateTime since, Pageable pageable);
} 