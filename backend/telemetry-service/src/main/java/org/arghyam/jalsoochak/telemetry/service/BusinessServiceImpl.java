package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    @Override
    public List<SampleDTO> getAllReadings() {
        log.info("Fetching all telemetry readings");
        return List.of(
                SampleDTO.builder().id(1L).meterId("METER-001").readingValue(150.5).build(),
                SampleDTO.builder().id(2L).meterId("METER-002").readingValue(230.8).build()
        );
    }
}
