package org.arghyam.jalsoochak.analytics.repository;

import org.arghyam.jalsoochak.analytics.entity.FactMeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FactMeterReadingRepository extends JpaRepository<FactMeterReading, Long> {

    List<FactMeterReading> findByTenantId(Integer tenantId);

    List<FactMeterReading> findBySchemeId(Integer schemeId);

    List<FactMeterReading> findByReadingDateBetween(LocalDate startDate, LocalDate endDate);

    List<FactMeterReading> findByTenantIdAndReadingDateBetween(Integer tenantId, LocalDate startDate, LocalDate endDate);

    List<FactMeterReading> findBySchemeIdAndReadingDateBetween(Integer schemeId, LocalDate startDate, LocalDate endDate);
}
