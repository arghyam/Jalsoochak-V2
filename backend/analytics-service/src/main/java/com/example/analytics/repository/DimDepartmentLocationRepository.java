package com.example.analytics.repository;

import com.example.analytics.entity.DimDepartmentLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimDepartmentLocationRepository extends JpaRepository<DimDepartmentLocation, Integer> {

    List<DimDepartmentLocation> findByTenantId(Integer tenantId);
}
