package com.example.tenant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor that reads the tenant code injected by the API gateway.
 * The gateway validates the Keycloak JWT, extracts the tenant_state_code claim,
 * and forwards it as the X-Tenant-Code header.
 */
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-Code";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantCode = request.getHeader(TENANT_HEADER);

        if (tenantCode != null && !tenantCode.isBlank()) {
            String schemaName = "tenant_" + tenantCode.toLowerCase().trim();
            TenantContext.setSchema(schemaName);
            log.debug("Tenant schema resolved from gateway header: {}", schemaName);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
