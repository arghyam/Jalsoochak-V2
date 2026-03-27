package org.arghyam.jalsoochak.tenant.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.arghyam.jalsoochak.tenant.validation.ValidChannelList;

import java.util.List;

/**
 * DTO for channel list configuration.
 * Supports: BFM, ELM, PDU, IOT, MAN
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class ChannelListConfigDTO implements ConfigValueDTO {
    @NotEmpty(message = "At least one channel must be selected")
    @Size(max = 5, message = "Maximum 5 channels allowed")
    @ValidChannelList
    private List<@NotEmpty(message = "Channel code cannot be empty") String> channels;
}
