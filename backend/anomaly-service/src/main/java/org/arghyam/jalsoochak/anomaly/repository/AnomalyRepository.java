package org.arghyam.jalsoochak.anomaly.repository;

import org.arghyam.jalsoochak.anomaly.entity.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {
}
