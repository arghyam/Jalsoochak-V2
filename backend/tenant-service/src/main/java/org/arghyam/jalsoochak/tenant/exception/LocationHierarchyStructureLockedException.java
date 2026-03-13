package org.arghyam.jalsoochak.tenant.exception;

/**
 * Thrown when a structural change (adding or removing levels) is attempted on a
 * location hierarchy that already has seeded data in the master table.
 * Mapped to HTTP 409 Conflict in GlobalExceptionHandler.
 */
public class LocationHierarchyStructureLockedException extends RuntimeException {
    public LocationHierarchyStructureLockedException(String hierarchyType, long seededCount) {
        super("Cannot modify the structure of " + hierarchyType + " location hierarchy: "
                + seededCount + " seeded record(s) exist. Only level name changes are permitted.");
    }
}
