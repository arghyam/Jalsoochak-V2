package org.arghyam.jalsoochak.scheme.repository;

public record SchemeLgdMappingCreateRecord(
        Integer schemeId,
        Integer parentLgdId,
        Integer parentLgdLevel,
        Integer createdBy,
        Integer updatedBy
) {
}

