package com.example.analytics.repository;

import com.example.analytics.entity.DimTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimTenantRepository extends JpaRepository<DimTenant, Integer> {

    List<DimTenant> findByStatus(Integer status);
}
