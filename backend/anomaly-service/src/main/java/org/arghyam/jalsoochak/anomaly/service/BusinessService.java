package org.arghyam.jalsoochak.anomaly.service;

import org.arghyam.jalsoochak.anomaly.dto.SampleDTO;

import java.util.List;

public interface BusinessService {

    List<SampleDTO> getAllAnomalies();
}
