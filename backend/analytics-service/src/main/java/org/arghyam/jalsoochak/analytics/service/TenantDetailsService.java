package org.arghyam.jalsoochak.analytics.service;

import org.arghyam.jalsoochak.analytics.dto.response.TenantDetailsResponse;

public interface TenantDetailsService {

    TenantDetailsResponse getTenantDetails(Integer tenantId, Integer parentLgdId);

    TenantDetailsResponse getTenantDetailsByParentDepartment(Integer tenantId, Integer parentDepartmentId);
}
