package com.example.analytics.repository;

import com.example.analytics.entity.FactEscalation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FactEscalationRepository extends JpaRepository<FactEscalation, Long> {

    List<FactEscalation> findByTenantId(Integer tenantId);

    List<FactEscalation> findBySchemeId(Integer schemeId);

    List<FactEscalation> findByResolutionStatus(Integer resolutionStatus);

    List<FactEscalation> findByTenantIdAndResolutionStatus(Integer tenantId, Integer resolutionStatus);
}
