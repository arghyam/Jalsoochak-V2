package com.example.message.service;

import com.example.message.dto.SampleDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class BusinessServiceImpl implements BusinessService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public List<SampleDTO> getAllNotifications() {
        log.info("Fetching all notifications (hardcoded)");
        return List.of(
                SampleDTO.builder()
                        .id(1L)
                        .recipient("admin@example.com")
                        .subject("Leak Alert")
                        .body("A leak has been detected in Zone 1")
                        .channel("EMAIL")
                        .status("SENT")
                        .sentAt(LocalDateTime.of(2026, 2, 15, 10, 30, 0).format(FMT))
                        .build(),
                SampleDTO.builder()
                        .id(2L)
                        .recipient("+919876543210")
                        .subject("Pressure Drop Warning")
                        .body("Pressure dropped below threshold in Zone 2")
                        .channel("WHATSAPP")
                        .status("SENT")
                        .sentAt(LocalDateTime.of(2026, 2, 15, 11, 0, 0).format(FMT))
                        .build(),
                SampleDTO.builder()
                        .id(3L)
                        .recipient("https://hooks.example.com/alerts")
                        .subject("Scheduled Maintenance")
                        .body("Maintenance window starts at 02:00 AM")
                        .channel("WEBHOOK")
                        .status("PENDING")
                        .sentAt(null)
                        .build(),
                SampleDTO.builder()
                        .id(4L)
                        .recipient("ops-team@example.com")
                        .subject("Anomaly Resolved")
                        .body("Leak in Zone 1 has been resolved")
                        .channel("EMAIL")
                        .status("SENT")
                        .sentAt(LocalDateTime.of(2026, 2, 16, 9, 15, 0).format(FMT))
                        .build()
        );
    }
}
