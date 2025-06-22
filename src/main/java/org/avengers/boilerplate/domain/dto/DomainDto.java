package org.avengers.boilerplate.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainDto {
    
    private Long id;
    
    @NotBlank(message = "Domain name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Active status is required")
    private Boolean active;
    
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;
    
    @Min(value = 1, message = "Check interval must be at least 1 minute")
    @Max(value = 10080, message = "Check interval cannot exceed 10080 minutes (1 week)")
    private Integer checkIntervalMinutes;
    
    private String webhookUrl;
    
    private String notificationEmail;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCheckAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextCheckAt;
    
    // Additional fields for API responses
    private SslCertificateDto latestCertificate;
    private Long totalChecks;
    private Long failedChecks;
} 