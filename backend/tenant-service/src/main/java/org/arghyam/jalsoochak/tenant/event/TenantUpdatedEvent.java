package org.arghyam.jalsoochak.tenant.event;

import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;

import lombok.Getter;

@Getter
public class TenantUpdatedEvent {
    private final TenantResponseDTO tenant;

    public TenantUpdatedEvent(TenantResponseDTO tenant) {
        this.tenant = tenant;
    }
}
