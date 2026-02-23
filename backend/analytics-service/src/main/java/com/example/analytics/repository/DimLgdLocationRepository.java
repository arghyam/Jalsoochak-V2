package com.example.analytics.repository;

import com.example.analytics.entity.DimLgdLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimLgdLocationRepository extends JpaRepository<DimLgdLocation, Integer> {

    List<DimLgdLocation> findByTenantId(Integer tenantId);
}
