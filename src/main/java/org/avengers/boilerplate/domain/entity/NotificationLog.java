package org.avengers.boilerplate.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs", indexes = {
    @Index(name = "idx_domain_id", columnList = "domain_id"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private Domain domain;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;
    
    @Column(name = "recipient", length = 500)
    private String recipient;
    
    @Column(name = "subject", length = 500)
    private String subject;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "response_code")
    private Integer responseCode;
    
    @Column(name = "response_message", length = 1000)
    private String responseMessage;
    
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    public enum NotificationType {
        WEBHOOK,
        EMAIL,
        SLACK
    }
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        RETRY
    }
} 