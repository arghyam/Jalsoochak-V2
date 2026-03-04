package org.arghyam.jalsoochak.tenant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.dto.internal.LocationLevelConfigDTO;

import java.util.List;

/**
 * DTO for location hierarchy configuration within a tenant.
 * Contains the structure (levels) of the location hierarchy (LGD or Department).
 * 
 * Example structure for LGD: Level 1 = State, Level 2 = District, Level 3 = Block, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location hierarchy configuration with level definitions")
public class LocationHierarchyResponseDTO {

    @Schema(
        description = "Hierarchy type: LGD (Local Government Directory) or DEPARTMENT",
        example = "LGD"
    )
    private String hierarchyType;

    @Schema(
        description = "List of hierarchy levels with multilingual names",
        example = "[{\"level\": 1, \"levelName\": [{\"languageId\": 1, \"title\": \"State\"}]}, {\"level\": 2, \"levelName\": [{\"languageId\": 1, \"title\": \"District\"}]}]"
    )
    private List<LocationLevelConfigDTO> levels;
}
