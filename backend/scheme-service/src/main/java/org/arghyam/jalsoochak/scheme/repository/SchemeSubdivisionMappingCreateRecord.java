package org.arghyam.jalsoochak.scheme.repository;

public record SchemeSubdivisionMappingCreateRecord(
        Integer schemeId,
        Integer parentDepartmentId,
        String parentDepartmentLevel,
        Integer createdBy,
        Integer updatedBy
) {
}
