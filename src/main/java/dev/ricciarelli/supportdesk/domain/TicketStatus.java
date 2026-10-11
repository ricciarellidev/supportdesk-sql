package dev.ricciarelli.supportdesk.domain;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    public boolean allows(TicketStatus next) {
        if (next == null) return false;
        return switch (this) {
            case OPEN -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == RESOLVED;
            case RESOLVED -> next == IN_PROGRESS || next == CLOSED;
            case CLOSED -> false;
        };
    }
}
