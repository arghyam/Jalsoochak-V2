package org.arghyam.jalsoochak.scheme.service;

import org.arghyam.jalsoochak.scheme.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    @Override
    public List<SampleDTO> getAllSchemes() {
        log.info("Fetching all schemes");
        return List.of(
                SampleDTO.builder().id(1L).schemeName("Rural Water Supply").schemeCode("RWS-001").channel(1).build(),
                SampleDTO.builder().id(2L).schemeName("Urban Pipeline Network").schemeCode("UPN-002").channel(2).build()
        );
    }
}
