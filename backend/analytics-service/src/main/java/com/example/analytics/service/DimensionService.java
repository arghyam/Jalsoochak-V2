package com.example.analytics.service;

import com.example.analytics.dto.event.DepartmentLocationEvent;
import com.example.analytics.dto.event.LgdLocationEvent;
import com.example.analytics.dto.event.SchemeEvent;
import com.example.analytics.dto.event.TenantEvent;
import com.example.analytics.dto.event.UserEvent;

public interface DimensionService {

    void upsertTenant(TenantEvent event);

    void upsertUser(UserEvent event);

    void upsertScheme(SchemeEvent event);

    void upsertLgdLocation(LgdLocationEvent event);

    void upsertDepartmentLocation(DepartmentLocationEvent event);
}
