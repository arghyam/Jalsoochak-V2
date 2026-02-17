package com.example.message.service;

import com.example.message.dto.NotificationRequest;

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
