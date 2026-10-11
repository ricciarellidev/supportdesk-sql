package dev.ricciarelli.supportdesk.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.Instant;

class PriorityTest {
    @ParameterizedTest
    @CsvSource({"CRITICAL,4", "HIGH,8", "MEDIUM,24", "LOW,72"})
    void calculatesDeadlinesInElapsedHours(Priority priority, long hours) {
        Instant friday = Instant.parse("2026-10-02T23:00:00Z");
        assertThat(Duration.between(friday, friday.plus(priority.resolutionWindow())))
                .isEqualTo(Duration.ofHours(hours));
    }
}
