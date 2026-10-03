package com.test.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;

/**
 * A meal that can appear on restaurant menus, e.g. "hamburger" or "fries (large)".
 * This is the shared catalogue concept: many restaurants can offer the same meal,
 * each with its own price and available quantity via {@link MenuItem}.
 */
@Entity
public class Meal extends PanacheEntity {

    /** Name of the meal, e.g. "hamburger w/bacon and cheese". */
    public String name;
}
