package com.example.tenant.service;

import com.example.tenant.dto.SampleDTO;

import java.util.List;

public interface BusinessService {

    List<SampleDTO> getAllTenants();
}
