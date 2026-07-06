package com.antra.evaluation.reporting_system;

import com.antra.evaluation.reporting_system.repo.LocalDateTimeConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LocalDateTimeConverterTest {

    private final LocalDateTimeConverter converter = new LocalDateTimeConverter();

    @Test
    public void roundTripsLocalDateTime() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 5, 12, 34, 56);
        String stored = converter.convert(now);
        assertEquals(now, converter.unconvert(stored));
    }

    @Test
    public void handlesNulls() {
        assertNull(converter.convert(null));
        assertNull(converter.unconvert(null));
    }
}
