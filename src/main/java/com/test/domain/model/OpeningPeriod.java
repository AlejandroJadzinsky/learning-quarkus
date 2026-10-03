package com.test.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.time.LocalTime;

/**
 * One weekly opening window of a {@link Schedule}, e.g. "Monday from 11:00 to 13:00".
 * <p>
 * Periods that cross midnight are stored as TWO rows (two-row approach):
 * "Saturday 19:00-23:59:59" and "Sunday 00:00-01:00". This keeps the invariant
 * open &lt;= close within every row, so an availability check is simply:
 * day matches AND openTime &lt;= now &lt;= closeTime.
 */
@Entity
public class OpeningPeriod extends PanacheEntity {

    /** The schedule this period belongs to (owning side of the relation). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Schedule schedule;

    /** Weekday this window applies to. */
    @Enumerated(EnumType.STRING)
    public DayOfWeek day;

    /** Opening time of the window (inclusive), e.g. 11:00. */
    public LocalTime openTime;

    /** Closing time of the window (inclusive), e.g. 13:00. Always >= openTime. */
    public LocalTime closeTime;
}
