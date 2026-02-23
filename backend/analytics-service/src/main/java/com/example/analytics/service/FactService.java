package com.example.analytics.service;

import com.example.analytics.dto.event.EscalationEvent;
import com.example.analytics.dto.event.MeterReadingEvent;
import com.example.analytics.dto.event.SchemePerformanceEvent;
import com.example.analytics.dto.event.WaterQuantityEvent;

public interface FactService {

    void ingestMeterReading(MeterReadingEvent event);

    void ingestWaterQuantity(WaterQuantityEvent event);

    void ingestEscalation(EscalationEvent event);

    void ingestSchemePerformance(SchemePerformanceEvent event);
}
