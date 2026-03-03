package org.arghyam.jalsoochak.anomaly.repository;

import org.arghyam.jalsoochak.anomaly.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
}
