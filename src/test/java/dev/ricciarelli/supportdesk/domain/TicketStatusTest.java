package dev.ricciarelli.supportdesk.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class TicketStatusTest {
    @ParameterizedTest
    @CsvSource({
        "OPEN,OPEN,false", "OPEN,IN_PROGRESS,true", "OPEN,RESOLVED,false", "OPEN,CLOSED,false",
        "IN_PROGRESS,OPEN,false", "IN_PROGRESS,IN_PROGRESS,false", "IN_PROGRESS,RESOLVED,true",
                "IN_PROGRESS,CLOSED,false",
        "RESOLVED,OPEN,false", "RESOLVED,IN_PROGRESS,true", "RESOLVED,RESOLVED,false",
                "RESOLVED,CLOSED,true",
        "CLOSED,OPEN,false", "CLOSED,IN_PROGRESS,false", "CLOSED,RESOLVED,false",
                "CLOSED,CLOSED,false"
    })
    void enforcesTheDocumentedLifecycle(TicketStatus from, TicketStatus to, boolean allowed) {
        assertThat(from.allows(to)).isEqualTo(allowed);
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void rejectsNullDestination(TicketStatus from) {
        assertThat(from.allows(null)).isFalse();
    }
}
