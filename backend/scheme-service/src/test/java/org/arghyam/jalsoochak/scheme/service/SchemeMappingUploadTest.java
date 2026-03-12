package org.arghyam.jalsoochak.scheme.service;

import org.arghyam.jalsoochak.scheme.auth.UploadAuthService;
import org.arghyam.jalsoochak.scheme.config.TenantContext;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadResponseDTO;
import org.arghyam.jalsoochak.scheme.exception.FileValidationException;
import org.arghyam.jalsoochak.scheme.repository.SchemeDbRepository;
import org.arghyam.jalsoochak.scheme.repository.SchemeLgdMappingCreateRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemeMappingUploadTest {

    @Mock
    SchemeDbRepository schemeDbRepository;

    @Mock
    UploadAuthService uploadAuthService;

    @Mock
    SchemeUploadChunkProcessor chunkProcessor;

    @InjectMocks
    SchemeServiceImpl schemeService;

    @Captor
    ArgumentCaptor<List<SchemeLgdMappingCreateRecord>> rowsCaptor;

    @BeforeEach
    void setUp() {
        TenantContext.setSchema("tenant_ka");
        when(uploadAuthService.requireStateAdminUserId(eq("tenant_ka"), eq("Bearer token"))).thenReturn(10);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void uploadSchemeMappings_insertsLgdMappings_andPopulatesCreatedByFromActor() {
        String csv = """
                scheme_id,parent_lgd_id,parent_lgd_level
                1,501,5
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scheme-mappings.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // Existence checks are now performed in batch to avoid N queries for large uploads.
        when(schemeDbRepository.findExistingSchemeIds(eq("tenant_ka"), anyList())).thenReturn(Set.of(1));
        when(schemeDbRepository.findExistingLgdLocationIds(eq("tenant_ka"), anyList())).thenReturn(Set.of(501));

        SchemeUploadResponseDTO res = schemeService.uploadSchemeMappings(file, "Bearer token");

        assertThat(res.getMessage()).isEqualTo("Scheme mappings uploaded successfully");
        assertThat(res.getTotalRows()).isEqualTo(1);
        assertThat(res.getUploadedRows()).isEqualTo(1);

        verify(chunkProcessor).insertMappingsChunk(eq("tenant_ka"), rowsCaptor.capture(), eq(List.of()));
        List<SchemeLgdMappingCreateRecord> rows = rowsCaptor.getValue();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().schemeId()).isEqualTo(1);
        assertThat(rows.getFirst().parentLgdId()).isEqualTo(501);
        assertThat(rows.getFirst().parentLgdLevel()).isEqualTo(5);
        assertThat(rows.getFirst().createdBy()).isEqualTo(10);
        assertThat(rows.getFirst().updatedBy()).isEqualTo(10);
    }

    @Test
    void uploadSchemeMappings_rejectsInvalidHeaders() {
        String csv = """
                scheme_id,village_id,subdivision_id
                1,501,301
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scheme-mappings.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> schemeService.uploadSchemeMappings(file, "Bearer token"))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("Invalid headers");

        verifyNoInteractions(schemeDbRepository);
    }

    @Test
    void uploadSchemeMappings_rejectsParentLgdLevelAbove6() {
        String csv = """
                scheme_id,parent_lgd_id,parent_lgd_level
                1,501,7
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scheme-mappings.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> schemeService.uploadSchemeMappings(file, "Bearer token"))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("Validation failed for uploaded file")
                .satisfies(ex -> {
                    FileValidationException fve = (FileValidationException) ex;
                    assertThat(fve.getErrors())
                            .anySatisfy(err -> {
                                assertThat(err.getField()).isEqualTo("parent_lgd_level");
                                assertThat(err.getMessage()).contains("between 1 and 6");
                            });
                });

        // Row-level validation fails before DB existence checks run.
        verifyNoInteractions(schemeDbRepository);
    }
}
