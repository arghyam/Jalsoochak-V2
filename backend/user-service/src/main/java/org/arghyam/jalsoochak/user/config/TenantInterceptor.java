package org.arghyam.jalsoochak.user.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_HEADER = "X-Tenant-Code";
    private static final Pattern SAFE_TENANT_CODE = Pattern.compile("^[A-Za-z0-9_]{1,32}$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantCode = request.getHeader(TENANT_HEADER);

        if (tenantCode != null) {
            String normalizedTenantCode = tenantCode.trim();
            if (!normalizedTenantCode.isEmpty() && !SAFE_TENANT_CODE.matcher(normalizedTenantCode).matches()) {
                log.warn("Rejected invalid tenant code format in header");
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Code format");
                return false;
            }
        }

        if (tenantCode != null && !tenantCode.isBlank()) {
            String schemaName = "tenant_" + tenantCode.toLowerCase().trim();
            TenantContext.setSchema(schemaName);
            log.debug("Resolved tenant schema from header: {}", schemaName);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }
}
