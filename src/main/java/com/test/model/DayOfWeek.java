package com.test.model;

/**
 * Days of the week used by the {@link OpeningPeriod} schedule.
 * Kept as our own enum (instead of java.time.DayOfWeek) so that it maps
 * cleanly to a MySQL column with simple, readable values.
 */
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
