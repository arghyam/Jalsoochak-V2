package org.arghyam.jalsoochak.tenant.event;

import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;

import lombok.Getter;

@Getter
public class TenantCreatedEvent {
    private final TenantResponseDTO tenant;
    private final String schemaName;

    public TenantCreatedEvent(TenantResponseDTO tenant, String schemaName) {
        this.tenant = tenant;
        this.schemaName = schemaName;
    }
}
