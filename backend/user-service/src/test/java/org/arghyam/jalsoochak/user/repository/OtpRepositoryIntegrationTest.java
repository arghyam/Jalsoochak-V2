package org.arghyam.jalsoochak.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.arghyam.jalsoochak.user.enums.OtpType;
import org.arghyam.jalsoochak.user.repository.records.OtpRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@DisplayName("OtpRepository Integration Tests")
class OtpRepositoryIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withInitScript("sql/test-schema.sql");

    @DynamicPropertySource
    static void dbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired OtpRepository otpRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private static final long USER_ID  = 1L;
    private static final int  TENANT_ID = 1;

    @BeforeEach
    void cleanOtpTable() {
        jdbcTemplate.execute("DELETE FROM common_schema.otp_table");
    }

    private Instant futureExpiry() {
        return Instant.now().plus(10, ChronoUnit.MINUTES);
    }

    @Nested
    @DisplayName("insertOtp / findActiveOtp")
    class InsertAndFind {

        @Test
        @DisplayName("finds an active OTP after insert")
        void findsActiveOtpAfterInsert() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "enc-otp", futureExpiry());

            Optional<OtpRow> found = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN);
            assertThat(found).isPresent();
            assertThat(found.get().encryptedOtp()).isEqualTo("enc-otp");
            assertThat(found.get().attemptCount()).isZero();
            assertThat(found.get().usedAt()).isNull();
        }

        @Test
        @DisplayName("returns empty when OTP is expired")
        void returnsEmptyForExpiredOtp() {
            Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
            jdbcTemplate.update("""
                    INSERT INTO common_schema.otp_table (otp, tenant_id, user_id, otp_type, expires_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, "enc-otp", TENANT_ID, USER_ID, "LOGIN",
                    java.sql.Timestamp.from(past));

            assertThat(otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN)).isEmpty();
        }

        @Test
        @DisplayName("partial unique index prevents two active OTPs for same user/tenant/type")
        void uniqueIndexPreventsDoubleInsert() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "otp-1", futureExpiry());

            assertThatThrownBy(() ->
                    otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "otp-2", futureExpiry()))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    @DisplayName("revokeActiveOtp")
    class Revoke {

        @Test
        @DisplayName("sets used_at so OTP is no longer findable")
        void revokedOtpNotFound() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "enc-otp", futureExpiry());
            otpRepository.revokeActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN);

            assertThat(otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN)).isEmpty();
        }

        @Test
        @DisplayName("allows new insert after revoke (unique index released)")
        void allowsInsertAfterRevoke() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "otp-1", futureExpiry());
            otpRepository.revokeActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN);
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "otp-2", futureExpiry());

            Optional<OtpRow> found = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN);
            assertThat(found).isPresent();
            assertThat(found.get().encryptedOtp()).isEqualTo("otp-2");
        }
    }

    @Nested
    @DisplayName("incrementAttemptCount / markUsed")
    class AttemptAndMarkUsed {

        @Test
        @DisplayName("incrementAttemptCount increases count by 1")
        void incrementsCount() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "enc-otp", futureExpiry());
            OtpRow before = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN).orElseThrow();

            otpRepository.incrementAttemptCount(before.id());

            OtpRow after = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN).orElseThrow();
            assertThat(after.attemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("markUsed removes OTP from active set")
        void markUsedRemovesFromActive() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "enc-otp", futureExpiry());
            OtpRow row = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN).orElseThrow();

            otpRepository.markUsed(row.id());

            assertThat(otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN)).isEmpty();
        }
    }

    @Nested
    @DisplayName("revertConsumption")
    class RevertConsumption {

        @Test
        @DisplayName("restores OTP to active after markUsed")
        void restoresActiveOtpAfterMarkUsed() {
            otpRepository.insertOtp(USER_ID, TENANT_ID, OtpType.LOGIN, "enc-otp", futureExpiry());
            OtpRow row = otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN).orElseThrow();
            otpRepository.markUsed(row.id());

            boolean reverted = otpRepository.revertConsumption(row.id());

            assertThat(reverted).isTrue();
            assertThat(otpRepository.findActiveOtp(USER_ID, TENANT_ID, OtpType.LOGIN)).isPresent();
        }

        @Test
        @DisplayName("returns false for an expired OTP (no revert)")
        void returnsFalseForExpiredOtp() {
            Instant past = Instant.now().minus(1, ChronoUnit.MINUTES);
            jdbcTemplate.update("""
                    INSERT INTO common_schema.otp_table (otp, tenant_id, user_id, otp_type, expires_at, used_at)
                    VALUES (?, ?, ?, ?, ?, NOW())
                    """, "enc-otp", TENANT_ID, USER_ID, "LOGIN",
                    java.sql.Timestamp.from(past));
            Long id = jdbcTemplate.queryForObject(
                    "SELECT id FROM common_schema.otp_table WHERE user_id = ?", Long.class, USER_ID);

            boolean reverted = otpRepository.revertConsumption(id);

            assertThat(reverted).isFalse();
        }
    }
}
