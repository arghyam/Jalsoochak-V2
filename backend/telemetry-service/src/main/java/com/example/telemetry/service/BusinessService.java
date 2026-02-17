package com.example.telemetry.service;

import com.example.telemetry.dto.SampleDTO;

import java.util.List;

public interface BusinessService {

    List<SampleDTO> getAllReadings();
}
