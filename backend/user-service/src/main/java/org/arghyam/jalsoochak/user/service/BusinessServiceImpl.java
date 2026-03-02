package org.arghyam.jalsoochak.user.service;

import org.arghyam.jalsoochak.user.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    @Override
    public List<SampleDTO> getAllUsers() {
        log.info("Fetching all users");
        return List.of(
                SampleDTO.builder().id(1L).name("John Doe").build(),
                SampleDTO.builder().id(2L).name("Jane Doe").build()
        );
    }
}
