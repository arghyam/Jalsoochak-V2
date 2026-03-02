package org.arghyam.jalsoochak.analytics.repository;

import org.arghyam.jalsoochak.analytics.entity.DimUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimUserRepository extends JpaRepository<DimUser, Integer> {

    List<DimUser> findByTenantId(Integer tenantId);
}
