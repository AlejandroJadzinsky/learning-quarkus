package com.test.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

/**
 * The activity schedule of a single {@link Restaurant}: the list of {@link OpeningPeriod}s
 * it is open. Acts as the intermediate entity between {@link Restaurant} and its periods,
 * mirroring the Restaurant -> Menu -> MenuItem structure.
 */
@Entity
public class Schedule extends PanacheEntity {

    /** The restaurant this schedule belongs to. Kept unique (one schedule per restaurant). */
    public Long restaurantId;

    /** Weekly opening windows. Periods that cross midnight are split into two rows. */
    @OneToMany(mappedBy = "schedule")
    public List<OpeningPeriod> periods = new ArrayList<>();
}
