package org.avengers.boilerplate.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.avengers.boilerplate.domain.dto.DomainDto;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.repository.DomainRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class SslMonitorIntegrationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Autowired
    private DomainRepository domainRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }
    
    @Test
    void testCreateAndRetrieveDomain() throws Exception {
        // Given
        DomainDto domainDto = DomainDto.builder()
                .name("test-integration.com")
                .description("Integration test domain")
                .active(true)
                .port(443)
                .checkIntervalMinutes(1440)
                .build();
        
        // When & Then - Create domain
        String response = mockMvc.perform(post("/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(domainDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-integration.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Extract domain ID from response
        DomainDto createdDomain = objectMapper.readValue(response, DomainDto.class);
        Long domainId = createdDomain.getId();
        
        // When & Then - Retrieve domain
        mockMvc.perform(get("/domains/{id}", domainId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test-integration.com"))
                .andExpect(jsonPath("$.description").value("Integration test domain"));
    }
    
    @Test
    void testDomainLifecycle() throws Exception {
        // Given
        DomainDto domainDto = DomainDto.builder()
                .name("lifecycle-test.com")
                .description("Lifecycle test domain")
                .active(true)
                .port(443)
                .checkIntervalMinutes(1440)
                .build();
        
        // When & Then - Create
        String response = mockMvc.perform(post("/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(domainDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        DomainDto createdDomain = objectMapper.readValue(response, DomainDto.class);
        Long domainId = createdDomain.getId();
        
        // When & Then - Update
        DomainDto updateDto = DomainDto.builder()
                .name("lifecycle-test.com")
                .description("Updated description")
                .active(true)
                .port(443)
                .checkIntervalMinutes(720)
                .build();
        
        mockMvc.perform(put("/domains/{id}", domainId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.checkIntervalMinutes").value(720));
        
        // When & Then - Delete (soft delete)
        mockMvc.perform(delete("/domains/{id}", domainId))
                .andExpect(status().isNoContent());
        
        // Verify domain is marked as inactive
        Domain deletedDomain = domainRepository.findById(domainId).orElse(null);
        assert deletedDomain != null;
        assert !deletedDomain.getActive();
    }
    
    @Test
    void testGetAllDomains() throws Exception {
        // Given - Create multiple domains
        createTestDomain("domain1.com", "Test Domain 1");
        createTestDomain("domain2.com", "Test Domain 2");
        createTestDomain("domain3.com", "Test Domain 3");
        
        // When & Then
        mockMvc.perform(get("/domains")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }
    
    @Test
    void testDomainStatistics() throws Exception {
        // Given - Create domains with different states
        createTestDomain("active1.com", "Active Domain 1");
        createTestDomain("active2.com", "Active Domain 2");
        
        Domain inactiveDomain = Domain.builder()
                .name("inactive.com")
                .description("Inactive Domain")
                .active(false)
                .port(443)
                .checkIntervalMinutes(1440)
                .build();
        domainRepository.save(inactiveDomain);
        
        // When & Then
        mockMvc.perform(get("/domains/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDomains").value(3))
                .andExpect(jsonPath("$.activeDomains").value(2))
                .andExpect(jsonPath("$.inactiveDomains").value(1));
    }
    
    @Test
    void testTriggerManualSslCheck() throws Exception {
        // Given
        Domain testDomain = createTestDomain("manual-check.com", "Manual Check Domain");
        
        // When & Then
        mockMvc.perform(post("/domains/{id}/check", testDomain.getId()))
                .andExpect(status().isAccepted());
    }
    
    @Test
    void testBulkSslCheck() throws Exception {
        // Given
        Domain domain1 = createTestDomain("bulk1.com", "Bulk Check Domain 1");
        Domain domain2 = createTestDomain("bulk2.com", "Bulk Check Domain 2");
        
        // When & Then
        mockMvc.perform(post("/domains/bulk-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.List.of(domain1.getId(), domain2.getId()))))
                .andExpect(status().isAccepted());
    }
    
    @Test
    void testCertificateEndpoints() throws Exception {
        // Given
        Domain testDomain = createTestDomain("cert-test.com", "Certificate Test Domain");
        
        // When & Then - Get certificates for domain
        mockMvc.perform(get("/domains/{id}/certificates", testDomain.getId())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        
        // When & Then - Get latest certificate
        mockMvc.perform(get("/domains/{id}/certificates/latest", testDomain.getId()))
                .andExpect(status().isNotFound()); // No certificates yet
    }
    
    @Test
    void testCertificateStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/certificates/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCertificates").exists())
                .andExpect(jsonPath("$.validCertificates").exists())
                .andExpect(jsonPath("$.expiredCertificates").exists());
    }
    
    @Test
    void testExpiringCertificates() throws Exception {
        // When & Then
        mockMvc.perform(get("/certificates/expiring")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    void testExpiredCertificates() throws Exception {
        // When & Then
        mockMvc.perform(get("/certificates/expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    private Domain createTestDomain(String name, String description) {
        Domain domain = Domain.builder()
                .name(name)
                .description(description)
                .active(true)
                .port(443)
                .checkIntervalMinutes(1440)
                .build();
        return domainRepository.save(domain);
    }
} 