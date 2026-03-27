package org.arghyam.jalsoochak.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PumpOperatorRef {
    private Long userId;
    private String phone;
}
