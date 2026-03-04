package org.arghyam.jalsoochak.telemetry.service;

public final class AnomalyConstants {

    private AnomalyConstants() {
    }

    public static final int TYPE_UNREADABLE_IMAGE = 1;
    public static final int TYPE_MANUAL_OVERRIDE = 2;
    public static final int TYPE_CONSECUTIVE_OVERRIDE_5_DAYS = 3;
    public static final int TYPE_DUPLICATE_IMAGE_SUBMISSION = 4;
    public static final int TYPE_READING_LESS_THAN_PREVIOUS = 5;

    public static final int STATUS_OPEN = 1;
}
