package com.example.tenant.service;

import com.example.tenant.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    @Override
    public List<SampleDTO> getAllTenants() {
        log.info("Fetching all tenants");
        return List.of(
                SampleDTO.builder().id(1L).name("Tenant A").build(),
                SampleDTO.builder().id(2L).name("Tenant B").build()
        );
    }
}
