package org.arghyam.jalsoochak.message.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleDTO {

    private Long id;
    private String recipient;
    private String subject;
    private String body;
    private String channel;   // WEBHOOK | EMAIL | WHATSAPP
    private String status;    // PENDING | SENT | FAILED
    private String sentAt;
}
