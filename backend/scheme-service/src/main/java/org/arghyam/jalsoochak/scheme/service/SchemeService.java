package org.arghyam.jalsoochak.scheme.service;

import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SchemeService {

    List<SchemeDTO> getAllSchemes();

    SchemeUploadResponseDTO uploadSchemes(MultipartFile file, String authorizationHeader);

    SchemeUploadResponseDTO uploadSchemeMappings(MultipartFile file, String authorizationHeader);
}
