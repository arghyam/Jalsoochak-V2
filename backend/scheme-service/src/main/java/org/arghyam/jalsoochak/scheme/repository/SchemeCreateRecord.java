package org.arghyam.jalsoochak.scheme.repository;

public record SchemeCreateRecord(
        String uuid,
        String stateSchemeId,
        String centreSchemeId,
        String schemeName,
        Integer fhtcCount,
        Integer plannedFhtc,
        Integer houseHoldCount,
        Double latitude,
        Double longitude,
        Integer channel,
        Integer workStatus,
        Integer operatingStatus,
        Integer createdBy,
        Integer updatedBy
) {
}
