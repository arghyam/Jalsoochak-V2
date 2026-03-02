package org.arghyam.jalsoochak.scheme.repository;

import org.arghyam.jalsoochak.scheme.entity.SampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleRepository extends JpaRepository<SampleEntity, Long> {
}
