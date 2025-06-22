package org.avengers.boilerplate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.avengers.boilerplate.domain.dto.DomainDto;
import org.avengers.boilerplate.domain.entity.Domain;
import org.avengers.boilerplate.repository.DomainRepository;
import org.avengers.boilerplate.repository.SslCertificateRepository;
import org.avengers.boilerplate.service.ScheduledSslCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DomainController.class)
class DomainControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DomainRepository domainRepository;
    
    @MockBean
    private SslCertificateRepository sslCertificateRepository;
    
    @MockBean
    private ScheduledSslCheckService scheduledSslCheckService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private Domain testDomain;
    private DomainDto testDomainDto;
    
    @BeforeEach
    void setUp() {
        testDomain = Domain.builder()
                .id(1L)
                .name("example.com")
                .description("Test domain")
                .active(true)
                .port(443)
                .checkIntervalMinutes(1440)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        testDomainDto = DomainDto.builder()
                .name("example.com")
                .description("Test domain")
                .active(true)
                .port(443)
                .checkIntervalMinutes(1440)
                .build();
    }
    
    @Test
    void testGetAllDomains() throws Exception {
        // Given
        Page<Domain> domainPage = new PageImpl<>(List.of(testDomain));
        when(domainRepository.findAll(any(PageRequest.class))).thenReturn(domainPage);
        when(sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(sslCertificateRepository.countByDomainIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        
        // When & Then
        mockMvc.perform(get("/domains")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("example.com"))
                .andExpect(jsonPath("$.content[0].active").value(true));
    }
    
    @Test
    void testGetDomainById() throws Exception {
        // Given
        when(domainRepository.findById(1L)).thenReturn(Optional.of(testDomain));
        when(sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(sslCertificateRepository.countByDomainIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        
        // When & Then
        mockMvc.perform(get("/domains/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }
    
    @Test
    void testGetDomainById_NotFound() throws Exception {
        // Given
        when(domainRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(get("/domains/999"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testCreateDomain() throws Exception {
        // Given
        when(domainRepository.existsByName("example.com")).thenReturn(false);
        when(domainRepository.save(any(Domain.class))).thenReturn(testDomain);
        when(sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(sslCertificateRepository.countByDomainIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        
        // When & Then
        mockMvc.perform(post("/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDomainDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("example.com"))
                .andExpect(jsonPath("$.active").value(true));
    }
    
    @Test
    void testCreateDomain_DuplicateName() throws Exception {
        // Given
        when(domainRepository.existsByName("example.com")).thenReturn(true);
        
        // When & Then
        mockMvc.perform(post("/domains")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDomainDto)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testUpdateDomain() throws Exception {
        // Given
        when(domainRepository.findById(1L)).thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class))).thenReturn(testDomain);
        when(sslCertificateRepository.findFirstByDomainIdOrderByCheckDateDesc(anyLong()))
                .thenReturn(Optional.empty());
        when(sslCertificateRepository.countByDomainIdAndStatus(anyLong(), any()))
                .thenReturn(0L);
        
        // When & Then
        mockMvc.perform(put("/domains/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDomainDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("example.com"));
    }
    
    @Test
    void testUpdateDomain_NotFound() throws Exception {
        // Given
        when(domainRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(put("/domains/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testDomainDto)))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testDeleteDomain() throws Exception {
        // Given
        when(domainRepository.findById(1L)).thenReturn(Optional.of(testDomain));
        when(domainRepository.save(any(Domain.class))).thenReturn(testDomain);
        
        // When & Then
        mockMvc.perform(delete("/domains/1"))
                .andExpect(status().isNoContent());
        
        verify(domainRepository).save(argThat(domain -> !domain.getActive()));
    }
    
    @Test
    void testDeleteDomain_NotFound() throws Exception {
        // Given
        when(domainRepository.findById(999L)).thenReturn(Optional.empty());
        
        // When & Then
        mockMvc.perform(delete("/domains/999"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testTriggerSslCheck() throws Exception {
        // Given
        when(domainRepository.existsById(1L)).thenReturn(true);
        doNothing().when(scheduledSslCheckService).performManualSslCheck(1L);
        
        // When & Then
        mockMvc.perform(post("/domains/1/check"))
                .andExpect(status().isAccepted());
        
        verify(scheduledSslCheckService).performManualSslCheck(1L);
    }
    
    @Test
    void testTriggerSslCheck_NotFound() throws Exception {
        // Given
        when(domainRepository.existsById(999L)).thenReturn(false);
        
        // When & Then
        mockMvc.perform(post("/domains/999/check"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void testTriggerBulkSslCheck() throws Exception {
        // Given
        List<Long> domainIds = List.of(1L, 2L, 3L);
        doNothing().when(scheduledSslCheckService).performBulkSslCheck(domainIds);
        
        // When & Then
        mockMvc.perform(post("/domains/bulk-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(domainIds)))
                .andExpect(status().isAccepted());
        
        verify(scheduledSslCheckService).performBulkSslCheck(domainIds);
    }
    
    @Test
    void testGetDomainStats() throws Exception {
        // Given
        when(domainRepository.countActiveDomains()).thenReturn(5L);
        when(domainRepository.countInactiveDomains()).thenReturn(2L);
        
        // When & Then
        mockMvc.perform(get("/domains/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDomains").value(7))
                .andExpect(jsonPath("$.activeDomains").value(5))
                .andExpect(jsonPath("$.inactiveDomains").value(2));
    }
} 