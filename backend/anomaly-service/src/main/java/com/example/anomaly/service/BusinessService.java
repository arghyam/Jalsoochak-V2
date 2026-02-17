package com.example.anomaly.service;

import com.example.anomaly.dto.SampleDTO;

import java.util.List;

public interface BusinessService {

    List<SampleDTO> getAllAnomalies();
}
