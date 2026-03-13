package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TenantConfigRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<String> findConfigValue(int tenantId, String key) {
        List<String> rows = jdbcTemplate.query(
                "SELECT config_value " +
                        "FROM common_schema.tenant_config_master_table " +
                        "WHERE tenant_id=? AND config_key=? " +
                        "ORDER BY updated_at DESC, id DESC LIMIT 1",
                (rs, n) -> rs.getString("config_value"),
                tenantId,
                key
        );
        return rows.stream().findFirst();
    }
}

