package org.arghyam.jalsoochak.tenant.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Response for the location hierarchy edit-constraints pre-flight check.
 * The UI uses this to decide whether to show structural editing options
 * (add/remove levels) or allow only level name changes.
 */
@Getter
@Builder
public class LocationHierarchyEditConstraintsResponseDTO {

    /** The hierarchy type this constraint applies to: LGD or DEPARTMENT. */
    private String hierarchyType;

    /**
     * Whether structural changes (adding or removing levels) are allowed.
     * False when seeded location data exists in the master table.
     */
    private boolean structuralChangesAllowed;

    /**
     * Total number of seeded records in the master table for this hierarchy.
     * Zero means the hierarchy is empty and full structural changes are permitted.
     */
    private long seededRecordCount;
}
