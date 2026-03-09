package org.arghyam.jalsoochak.scheme.repository;

public record SchemeVillageMappingCreateRecord(
        Integer schemeId,
        Integer parentLgdId,
        String parentLgdLevel,
        Integer createdBy,
        Integer updatedBy
) {
}
