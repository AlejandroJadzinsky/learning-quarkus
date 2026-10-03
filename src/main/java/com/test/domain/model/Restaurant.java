package com.test.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 * A restaurant in the meal delivery system.
 * Each restaurant owns exactly one {@link Menu} (what it sells) and
 * one {@link Schedule} (when it is open).
 */
@Entity
public class Restaurant extends PanacheEntity {

    /** Display name of the restaurant, e.g. "Burger Palace". */
    public String name;
}
