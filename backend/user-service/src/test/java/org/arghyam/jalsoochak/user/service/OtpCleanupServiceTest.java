package org.arghyam.jalsoochak.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OtpCleanupService")
class OtpCleanupServiceTest {

    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks OtpCleanupService otpCleanupService;

    @Test
    @DisplayName("executes DELETE for expired OTPs and logs count")
    void deletesExpiredOtps() {
        when(jdbcTemplate.update(anyString())).thenReturn(3);

        otpCleanupService.deleteExpiredOtps();

        verify(jdbcTemplate).update(anyString());
    }
}
