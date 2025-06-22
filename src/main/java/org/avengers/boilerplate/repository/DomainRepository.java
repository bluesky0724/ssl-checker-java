package org.avengers.boilerplate.repository;

import org.avengers.boilerplate.domain.entity.Domain;
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
public interface DomainRepository extends JpaRepository<Domain, Long> {
    
    Optional<Domain> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Domain> findByActiveTrue();
    
    List<Domain> findByActiveTrueAndNextCheckAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT d FROM Domain d WHERE d.active = true AND (d.nextCheckAt IS NULL OR d.nextCheckAt <= :dateTime)")
    List<Domain> findDomainsReadyForCheck(@Param("dateTime") LocalDateTime dateTime);
    
    @Query("SELECT d FROM Domain d WHERE d.active = true AND d.name LIKE %:searchTerm%")
    Page<Domain> findActiveDomainsByNameContaining(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT COUNT(d) FROM Domain d WHERE d.active = true")
    long countActiveDomains();
    
    @Query("SELECT COUNT(d) FROM Domain d WHERE d.active = false")
    long countInactiveDomains();
    
    @Query("SELECT d FROM Domain d WHERE d.active = true ORDER BY d.nextCheckAt ASC")
    List<Domain> findActiveDomainsOrderByNextCheck();
} 