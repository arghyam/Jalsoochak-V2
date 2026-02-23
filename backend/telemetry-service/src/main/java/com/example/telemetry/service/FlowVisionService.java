package com.example.telemetry.service;

import com.example.telemetry.dto.response.FlowVisionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FlowVisionService {
    public FlowVisionResult extractReading(String imageUrl) {
        log.warn("FlowVision integration is not configured. OCR skipped for image: {}", imageUrl);
        return null;
    }
}
