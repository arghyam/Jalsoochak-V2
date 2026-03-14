package org.arghyam.jalsoochak.user.repository;

import lombok.RequiredArgsConstructor;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorDetailsDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceDTO;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorReadingComplianceRowDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PublicPumpOperatorRepository {

    private final JdbcTemplate jdbcTemplate;

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^[a-z_][a-z0-9_]*$")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
    }

    public PumpOperatorDetailsDTO findPumpOperatorById(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);
        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title,
                       u.email,
                       u.phone_number,
                       u.status,
                       ut.c_name AS role
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                LIMIT 1
                """, schemaName);
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> PumpOperatorDetailsDTO.builder()
                    .id(rs.getLong("id"))
                    .uuid(rs.getString("uuid"))
                    .name(rs.getString("title"))
                    .email(rs.getString("email"))
                    .phoneNumber(rs.getString("phone_number"))
                    .status((Integer) rs.getObject("status"))
                    .build(), pumpOperatorId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public PumpOperatorReadingComplianceDTO getReadingCompliance(String schemaName, long pumpOperatorId) {
        validateSchemaName(schemaName);

        // If the operator has no readings, lastSubmissionAt/confirmedReading will be null.
        String sql = String.format("""
                SELECT u.title AS name,
                       fr.last_submission_at,
                       fr.confirmed_reading
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT reading_at AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY reading_at DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND u.id = ?
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                LIMIT 1
                """, schemaName, schemaName);

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                Timestamp ts = (Timestamp) rs.getObject("last_submission_at");
                LocalDateTime lastSubmissionAt = ts == null ? null : ts.toLocalDateTime();
                BigDecimal confirmed = (BigDecimal) rs.getObject("confirmed_reading");
                return PumpOperatorReadingComplianceDTO.builder()
                        .name(rs.getString("name"))
                        .lastSubmissionAt(lastSubmissionAt)
                        .confirmedReading(confirmed)
                        .build();
            }, pumpOperatorId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    public List<PumpOperatorReadingComplianceRowDTO> listReadingCompliance(String schemaName) {
        validateSchemaName(schemaName);

        String sql = String.format("""
                SELECT u.id,
                       u.uuid,
                       u.title AS name,
                       fr.last_submission_at,
                       fr.confirmed_reading
                FROM %s.user_table u
                LEFT JOIN common_schema.user_type_master_table ut
                  ON ut.id = u.user_type
                LEFT JOIN LATERAL (
                    SELECT reading_at AS last_submission_at, confirmed_reading
                    FROM %s.flow_reading_table
                    WHERE deleted_at IS NULL
                      AND created_by = u.id
                    ORDER BY reading_at DESC, id DESC
                    LIMIT 1
                ) fr ON true
                WHERE u.deleted_at IS NULL
                  AND upper(COALESCE(ut.c_name, '')) = 'PUMP_OPERATOR'
                ORDER BY u.id DESC
                """, schemaName, schemaName);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Timestamp ts = (Timestamp) rs.getObject("last_submission_at");
            LocalDateTime lastSubmissionAt = ts == null ? null : ts.toLocalDateTime();
            BigDecimal confirmed = (BigDecimal) rs.getObject("confirmed_reading");
            return PumpOperatorReadingComplianceRowDTO.builder()
                    .id(rs.getLong("id"))
                    .uuid(rs.getString("uuid"))
                    .name(rs.getString("name"))
                    .lastSubmissionAt(lastSubmissionAt)
                    .confirmedReading(confirmed)
                    .build();
        });
    }
}
