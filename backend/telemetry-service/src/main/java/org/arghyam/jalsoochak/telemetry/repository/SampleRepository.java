package org.arghyam.jalsoochak.telemetry.repository;

import org.arghyam.jalsoochak.telemetry.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
}
