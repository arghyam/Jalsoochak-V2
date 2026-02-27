package org.arghyam.jalsoochak.telemetry.service;

import org.arghyam.jalsoochak.telemetry.dto.SampleDTO;

import java.util.List;

public interface BusinessService {

    List<SampleDTO> getAllReadings();
}
