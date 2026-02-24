package org.arghyam.jalsoochak.message.service;

import org.arghyam.jalsoochak.message.dto.NotificationRequest;

/**
 * Orchestrates notification delivery by routing requests to the
 * appropriate channel implementation.
 */
public interface NotificationService {

    /**
     * Dispatch a notification to the requested channel.
     *
     * @param request notification payload including channel type
     * @return result message
     */
    String send(NotificationRequest request);
}
