package com.test.model;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

/**
 * One entry of a {@link Menu}: links a {@link Meal} to the {@link Menu} that offers it,
 * carrying the price and the remaining sellable quantity for that restaurant.
 * The same meal can appear on many menus with different prices/quantities.
 */
@Entity
public class MenuItem extends PanacheEntity {

    /** The menu this item belongs to (owning side of the relation). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Menu menu;

    /** The meal being offered (shared catalogue entry). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    public Meal meal;

    /** Price charged by this restaurant for this meal, e.g. 5.50. */
    public BigDecimal price;

    /** Quantity this restaurant can still sell of this meal, e.g. 60. Zero means sold out. */
    public int availableQuantity;
}
