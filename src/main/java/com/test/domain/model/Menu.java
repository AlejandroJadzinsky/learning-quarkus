package com.test.domain.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu of a single {@link Restaurant}: the list of {@link MenuItem}s it offers.
 * Acts as the intermediate entity between {@link Restaurant} and its items, so a
 * restaurant "has one menu" and a menu "has many items".
 */
@Entity
public class Menu extends PanacheEntity {

    /** The restaurant that owns this menu. Kept unique (one menu per restaurant). */
    public Long restaurantId;

    /** The offered meals with their per-restaurant price and available quantity. */
    @OneToMany(mappedBy = "menu")
    public List<MenuItem> items = new ArrayList<>();
}
