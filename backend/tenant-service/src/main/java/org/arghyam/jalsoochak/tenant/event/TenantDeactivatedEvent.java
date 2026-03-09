package org.arghyam.jalsoochak.tenant.event;

import org.arghyam.jalsoochak.tenant.dto.response.TenantResponseDTO;

import lombok.Getter;

@Getter
public class TenantDeactivatedEvent {
    private final TenantResponseDTO tenant;

    public TenantDeactivatedEvent(TenantResponseDTO tenant) {
        this.tenant = tenant;
    }
}
