package org.avengers.boilerplate.repository;

import org.avengers.boilerplate.domain.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    
    List<NotificationLog> findByDomainIdOrderByCreatedAtDesc(Long domainId);
    
    List<NotificationLog> findByStatus(NotificationLog.NotificationStatus status);
    
    List<NotificationLog> findByStatusAndNextRetryAtBefore(NotificationLog.NotificationStatus status, LocalDateTime dateTime);
    
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.domain.id = :domainId ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByDomainId(@Param("domainId") Long domainId, Pageable pageable);
    
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.type = :type ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findByType(@Param("type") NotificationLog.NotificationType type, Pageable pageable);
    
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.createdAt >= :since ORDER BY nl.createdAt DESC")
    Page<NotificationLog> findRecentNotifications(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.status = :status")
    long countByStatus(@Param("status") NotificationLog.NotificationStatus status);
    
    @Query("SELECT COUNT(nl) FROM NotificationLog nl WHERE nl.type = :type")
    long countByType(@Param("type") NotificationLog.NotificationType type);
    
    @Query("SELECT nl FROM NotificationLog nl WHERE nl.domain.id = :domainId AND nl.status = 'FAILED' ORDER BY nl.createdAt DESC")
    List<NotificationLog> findFailedNotificationsByDomainId(@Param("domainId") Long domainId);
} 