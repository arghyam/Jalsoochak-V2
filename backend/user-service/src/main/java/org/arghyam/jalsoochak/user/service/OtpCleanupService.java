package org.arghyam.jalsoochak.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled housekeeping for the OTP table.
 * Deletes rows that expired more than 24 hours ago.
 * Runs daily at 03:00 server time to avoid peak load windows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpCleanupService {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredOtps() {
        try {
            int deleted = jdbcTemplate.update("""
                    DELETE FROM common_schema.otp_table
                    WHERE expires_at < NOW() - INTERVAL '1 day'
                    """);
            log.info("OTP cleanup: deleted {} expired row(s)", deleted);
        } catch (Exception e) {
            log.error("OTP cleanup failed — will retry on next scheduled run", e);
        }
    }
}
