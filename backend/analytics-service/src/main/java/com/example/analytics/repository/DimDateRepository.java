package com.example.analytics.repository;

import com.example.analytics.entity.DimDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DimDateRepository extends JpaRepository<DimDate, Integer> {

    Optional<DimDate> findByFullDate(LocalDate fullDate);
}
