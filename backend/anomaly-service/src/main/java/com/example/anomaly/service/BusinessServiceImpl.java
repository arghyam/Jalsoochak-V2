package com.example.anomaly.service;

import com.example.anomaly.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    @Override
    public List<SampleDTO> getAllAnomalies() {
        log.info("Fetching all anomalies");
        return List.of(
                SampleDTO.builder().id(1L).type("Leak Detection").build(),
                SampleDTO.builder().id(2L).type("Pressure Drop").build()
        );
    }
}
