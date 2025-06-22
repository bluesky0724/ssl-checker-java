package org.avengers.boilerplate.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "domains", indexes = {
    @Index(name = "idx_domain_name", columnList = "name"),
    @Index(name = "idx_domain_active", columnList = "active")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Domain {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Domain name is required")
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    @NotNull(message = "Active status is required")
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(name = "port")
    @Builder.Default
    private Integer port = 443;
    
    @Column(name = "check_interval_minutes")
    @Builder.Default
    private Integer checkIntervalMinutes = 1440; // 24 hours
    
    @Column(name = "webhook_url")
    private String webhookUrl;
    
    @Column(name = "notification_email")
    private String notificationEmail;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "last_check_at")
    private LocalDateTime lastCheckAt;
    
    @Column(name = "next_check_at")
    private LocalDateTime nextCheckAt;
} 