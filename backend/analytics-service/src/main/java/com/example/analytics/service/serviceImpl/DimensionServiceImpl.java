package com.example.analytics.service.serviceImpl;

import com.example.analytics.dto.event.DepartmentLocationEvent;
import com.example.analytics.dto.event.LgdLocationEvent;
import com.example.analytics.dto.event.SchemeEvent;
import com.example.analytics.dto.event.TenantEvent;
import com.example.analytics.dto.event.UserEvent;
import com.example.analytics.entity.DimDepartmentLocation;
import com.example.analytics.entity.DimLgdLocation;
import com.example.analytics.entity.DimScheme;
import com.example.analytics.entity.DimTenant;
import com.example.analytics.entity.DimUser;
import com.example.analytics.repository.DimDepartmentLocationRepository;
import com.example.analytics.repository.DimLgdLocationRepository;
import com.example.analytics.repository.DimSchemeRepository;
import com.example.analytics.repository.DimTenantRepository;
import com.example.analytics.repository.DimUserRepository;
import com.example.analytics.service.DimensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DimensionServiceImpl implements DimensionService {

    private final DimTenantRepository dimTenantRepository;
    private final DimUserRepository dimUserRepository;
    private final DimSchemeRepository dimSchemeRepository;
    private final DimLgdLocationRepository dimLgdLocationRepository;
    private final DimDepartmentLocationRepository dimDepartmentLocationRepository;

    @Override
    @Transactional
    public void upsertTenant(TenantEvent event) {
        DimTenant tenant = dimTenantRepository.findById(event.getTenantId())
                .orElse(DimTenant.builder()
                        .tenantId(event.getTenantId())
                        .createdAt(LocalDateTime.now())
                        .build());

        tenant.setStateCode(event.getStateCode());
        tenant.setTitle(event.getTitle());
        tenant.setCountryCode(event.getCountryCode() != null ? event.getCountryCode() : "IN");
        tenant.setStatus(event.getStatus());
        tenant.setUpdatedAt(LocalDateTime.now());

        dimTenantRepository.save(tenant);
        log.info("Upserted dim_tenant_table [id={}]", event.getTenantId());
    }

    @Override
    @Transactional
    public void upsertUser(UserEvent event) {
        DimUser user = dimUserRepository.findById(event.getUserId())
                .orElse(DimUser.builder()
                        .userId(event.getUserId())
                        .createdAt(LocalDateTime.now())
                        .build());

        user.setTenantId(event.getTenantId());
        user.setEmail(event.getEmail());
        user.setUserType(event.getUserType());
        user.setUpdatedAt(LocalDateTime.now());

        dimUserRepository.save(user);
        log.info("Upserted dim_user_table [id={}]", event.getUserId());
    }

    @Override
    @Transactional
    public void upsertScheme(SchemeEvent event) {
        DimScheme scheme = dimSchemeRepository.findById(event.getSchemeId())
                .orElse(DimScheme.builder()
                        .schemeId(event.getSchemeId())
                        .createdAt(LocalDateTime.now())
                        .build());

        scheme.setTenantId(event.getTenantId());
        scheme.setSchemeName(event.getSchemeName());
        scheme.setStateSchemeId(event.getStateSchemeId());
        scheme.setCentreSchemeId(event.getCentreSchemeId());
        scheme.setLongitude(event.getLongitude());
        scheme.setLatitude(event.getLatitude());

        scheme.setParentLgdLocationId(event.getParentLgdLocationId());
        scheme.setLevel1LgdId(event.getLevel1LgdId());
        scheme.setLevel2LgdId(event.getLevel2LgdId());
        scheme.setLevel3LgdId(event.getLevel3LgdId());
        scheme.setLevel4LgdId(event.getLevel4LgdId());
        scheme.setLevel5LgdId(event.getLevel5LgdId());
        scheme.setLevel6LgdId(event.getLevel6LgdId());

        scheme.setParentDepartmentLocationId(event.getParentDepartmentLocationId());
        scheme.setLevel1DeptId(event.getLevel1DeptId());
        scheme.setLevel2DeptId(event.getLevel2DeptId());
        scheme.setLevel3DeptId(event.getLevel3DeptId());
        scheme.setLevel4DeptId(event.getLevel4DeptId());
        scheme.setLevel5DeptId(event.getLevel5DeptId());
        scheme.setLevel6DeptId(event.getLevel6DeptId());

        scheme.setStatus(event.getStatus());
        scheme.setUpdatedAt(LocalDateTime.now());

        dimSchemeRepository.save(scheme);
        log.info("Upserted dim_scheme_table [id={}]", event.getSchemeId());
    }

    @Override
    @Transactional
    public void upsertLgdLocation(LgdLocationEvent event) {
        DimLgdLocation loc = dimLgdLocationRepository.findById(event.getLgdId())
                .orElse(DimLgdLocation.builder()
                        .lgdId(event.getLgdId())
                        .createdAt(LocalDateTime.now())
                        .build());

        loc.setTenantId(event.getTenantId());
        loc.setLgdCode(event.getLgdCode());
        loc.setLgdCName(event.getLgdCName());
        loc.setTitle(event.getTitle());
        loc.setLgdLevel(event.getLgdLevel());
        loc.setLevel1LgdId(event.getLevel1LgdId());
        loc.setLevel2LgdId(event.getLevel2LgdId());
        loc.setLevel3LgdId(event.getLevel3LgdId());
        loc.setLevel4LgdId(event.getLevel4LgdId());
        loc.setLevel5LgdId(event.getLevel5LgdId());
        loc.setLevel6LgdId(event.getLevel6LgdId());
        loc.setGeom(parseGeoJson(event.getGeom()));
        loc.setUpdatedAt(LocalDateTime.now());

        dimLgdLocationRepository.save(loc);
        log.info("Upserted dim_lgd_location_table [id={}]", event.getLgdId());
    }

    @Override
    @Transactional
    public void upsertDepartmentLocation(DepartmentLocationEvent event) {
        DimDepartmentLocation dept = dimDepartmentLocationRepository.findById(event.getDepartmentId())
                .orElse(DimDepartmentLocation.builder()
                        .departmentId(event.getDepartmentId())
                        .createdAt(LocalDateTime.now())
                        .build());

        dept.setTenantId(event.getTenantId());
        dept.setDepartmentCName(event.getDepartmentCName());
        dept.setTitle(event.getTitle());
        dept.setDepartmentLevel(event.getDepartmentLevel());
        dept.setLevel1DeptId(event.getLevel1DeptId());
        dept.setLevel2DeptId(event.getLevel2DeptId());
        dept.setLevel3DeptId(event.getLevel3DeptId());
        dept.setLevel4DeptId(event.getLevel4DeptId());
        dept.setLevel5DeptId(event.getLevel5DeptId());
        dept.setLevel6DeptId(event.getLevel6DeptId());
        dept.setUpdatedAt(LocalDateTime.now());

        dimDepartmentLocationRepository.save(dept);
        log.info("Upserted dim_department_location_table [id={}]", event.getDepartmentId());
    }

    private Geometry parseGeoJson(String geoJson) {
        if (geoJson == null || geoJson.isBlank()) return null;
        try {
            GeoJsonReader reader = new GeoJsonReader();
            return reader.read(geoJson);
        } catch (Exception e) {
            log.warn("Could not parse GeoJSON: {}", e.getMessage());
            return null;
        }
    }
}
