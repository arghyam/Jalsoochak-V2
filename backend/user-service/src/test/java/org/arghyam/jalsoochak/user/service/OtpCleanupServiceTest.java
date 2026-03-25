package org.arghyam.jalsoochak.user.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpCleanupService")
class OtpCleanupServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks OtpCleanupService otpCleanupService;

    @Test
    @DisplayName("executes DELETE for expired OTPs and logs count")
    void deletesExpiredOtps() {
        when(jdbcTemplate.update(argThat((String sql) -> sql.contains("DELETE") && sql.contains("otp_table")))).thenReturn(3);

        otpCleanupService.deleteExpiredOtps();

        verify(jdbcTemplate).update(argThat((String sql) -> sql.contains("DELETE") && sql.contains("otp_table")));
    }
}
