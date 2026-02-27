package org.arghyam.jalsoochak.message.service;

import org.arghyam.jalsoochak.message.channel.NotificationChannel;
import org.arghyam.jalsoochak.message.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final Map<String, NotificationChannel> channelMap;

    public NotificationServiceImpl(List<NotificationChannel> channels) {
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(
                        ch -> ch.channelType().toUpperCase(),
                        Function.identity()
                ));
        log.info("Registered notification channels: {}", channelMap.keySet());
    }

    @Override
    public String send(NotificationRequest request) {
        String channelType = request.getChannel() != null
                ? request.getChannel().toUpperCase()
                : "WEBHOOK";

        NotificationChannel channel = channelMap.get(channelType);
        if (channel == null) {
            String msg = "Unsupported notification channel: " + channelType
                    + ". Available: " + channelMap.keySet();
            log.warn(msg);
            return msg;
        }

        boolean success = channel.send(request);
        String status = success ? "SENT" : "FAILED";
        log.info("Notification via {} → {}", channelType, status);
        return "Notification via " + channelType + " → " + status;
    }
}
