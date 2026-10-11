package dev.ricciarelli.supportdesk.domain;

import java.time.Duration;

public enum Priority {
    LOW(72),
    MEDIUM(24),
    HIGH(8),
    CRITICAL(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public Duration resolutionWindow() {
        return Duration.ofHours(slaHours);
    }
}
