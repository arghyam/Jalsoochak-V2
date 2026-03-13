package org.arghyam.jalsoochak.analytics.service.serviceImpl;

import org.arghyam.jalsoochak.analytics.dto.event.DepartmentLocationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.LgdLocationEvent;
import org.arghyam.jalsoochak.analytics.dto.event.SchemeEvent;
import org.arghyam.jalsoochak.analytics.dto.event.TenantEvent;
import org.arghyam.jalsoochak.analytics.dto.event.UserEvent;
import org.arghyam.jalsoochak.analytics.entity.DimDepartmentLocation;
import org.arghyam.jalsoochak.analytics.entity.DimLgdLocation;
import org.arghyam.jalsoochak.analytics.entity.DimScheme;
import org.arghyam.jalsoochak.analytics.entity.DimTenant;
import org.arghyam.jalsoochak.analytics.entity.DimUser;
import org.arghyam.jalsoochak.analytics.repository.DimDepartmentLocationRepository;
import org.arghyam.jalsoochak.analytics.repository.DimLgdLocationRepository;
import org.arghyam.jalsoochak.analytics.repository.DimSchemeRepository;
import org.arghyam.jalsoochak.analytics.repository.DimTenantRepository;
import org.arghyam.jalsoochak.analytics.repository.DimUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DimensionServiceImplTest {

    @Mock
    private DimTenantRepository dimTenantRepository;
    @Mock
    private DimUserRepository dimUserRepository;
    @Mock
    private DimSchemeRepository dimSchemeRepository;
    @Mock
    private DimLgdLocationRepository dimLgdLocationRepository;
    @Mock
    private DimDepartmentLocationRepository dimDepartmentLocationRepository;

    @InjectMocks
    private DimensionServiceImpl service;

    @Test
    void upsertTenant_setsDefaultsAndSaves() {
        TenantEvent event = new TenantEvent();
        event.setTenantId(1);
        event.setStateCode("mp");
        event.setTitle("Madhya Pradesh");
        event.setCountryCode(null);
        event.setStatus(1);
        when(dimTenantRepository.findById(1)).thenReturn(Optional.empty());

        service.upsertTenant(event);

        ArgumentCaptor<DimTenant> captor = ArgumentCaptor.forClass(DimTenant.class);
        verify(dimTenantRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCountryCode()).isEqualTo("IN");
        assertThat(captor.getValue().getTenantId()).isEqualTo(1);
    }

    @Test
    void upsertUser_updatesExistingAndSaves() {
        UserEvent event = new UserEvent();
        event.setUserId(11);
        event.setTenantId(1);
        event.setEmail("user@test.local");
        event.setUserType(2);
        when(dimUserRepository.findById(11)).thenReturn(Optional.of(DimUser.builder().userId(11).build()));

        service.upsertUser(event);

        ArgumentCaptor<DimUser> captor = ArgumentCaptor.forClass(DimUser.class);
        verify(dimUserRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@test.local");
        assertThat(captor.getValue().getTenantId()).isEqualTo(1);
    }

    @Test
    void upsertScheme_mapsHierarchyAndSaves() {
        SchemeEvent event = new SchemeEvent();
        event.setSchemeId(1001);
        event.setTenantId(1);
        event.setSchemeName("Scheme-A");
        event.setStateSchemeId(10);
        event.setCentreSchemeId(20);
        event.setParentLgdLocationId(100);
        event.setLevel1LgdId(100);
        event.setLevel2LgdId(101);
        event.setParentDepartmentLocationId(200);
        event.setLevel1DeptId(200);
        event.setLevel2DeptId(201);
        event.setStatus(1);
        when(dimSchemeRepository.findById(1001)).thenReturn(Optional.empty());

        service.upsertScheme(event);

        ArgumentCaptor<DimScheme> captor = ArgumentCaptor.forClass(DimScheme.class);
        verify(dimSchemeRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getSchemeId()).isEqualTo(1001);
        assertThat(captor.getValue().getLevel2LgdId()).isEqualTo(101);
        assertThat(captor.getValue().getLevel2DeptId()).isEqualTo(201);
    }

    @Test
    void upsertLgdLocation_whenInvalidGeoJson_setsGeomNullAndSaves() {
        LgdLocationEvent event = new LgdLocationEvent();
        event.setLgdId(101);
        event.setTenantId(1);
        event.setLgdCode("L101");
        event.setLgdCName("Child A");
        event.setTitle("Child A");
        event.setLgdLevel(2);
        event.setLevel1LgdId(100);
        event.setLevel2LgdId(101);
        event.setGeom("{invalid}");
        when(dimLgdLocationRepository.findById(101)).thenReturn(Optional.empty());

        service.upsertLgdLocation(event);

        ArgumentCaptor<DimLgdLocation> captor = ArgumentCaptor.forClass(DimLgdLocation.class);
        verify(dimLgdLocationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getGeom()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("Child A");
    }

    @Test
    void upsertDepartmentLocation_mapsFieldsAndSaves() {
        DepartmentLocationEvent event = new DepartmentLocationEvent();
        event.setDepartmentId(201);
        event.setTenantId(1);
        event.setDepartmentCName("Dept A");
        event.setTitle("Department A");
        event.setDepartmentLevel(2);
        event.setLevel1DeptId(200);
        event.setLevel2DeptId(201);
        when(dimDepartmentLocationRepository.findById(201)).thenReturn(Optional.empty());

        service.upsertDepartmentLocation(event);

        ArgumentCaptor<DimDepartmentLocation> captor = ArgumentCaptor.forClass(DimDepartmentLocation.class);
        verify(dimDepartmentLocationRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getDepartmentLevel()).isEqualTo(2);
        assertThat(captor.getValue().getLevel2DeptId()).isEqualTo(201);
    }
}
