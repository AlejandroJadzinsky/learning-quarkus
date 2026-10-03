package com.test.domain.repositories;

import com.test.domain.model.DayOfWeek;
import com.test.domain.model.Meal;
import com.test.domain.model.MenuItem;
import com.test.domain.model.Menu;
import com.test.domain.model.OpeningPeriod;
import com.test.domain.model.Restaurant;
import com.test.domain.model.Schedule;
import io.smallrye.mutiny.Uni;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository port for everything that can be done on the {@code domain/model} classes:
 * {@link Restaurant}, {@link Menu}, {@link MenuItem}, {@link Meal}, {@link Schedule}
 * and {@link OpeningPeriod}.
 * <p>
 * Following the hexagonal layout of this project, this package only defines the PORT
 * (interface). The adapter (for instance a Panache/Hibernate-Reactive implementation)
 * lives in {@code infrastructure} and is wired in through dependency injection.
 * <p>
 * All operations are reactive: they return {@link Uni}s, which the caller must subscribe
 * to inside a Hibernate session (the infrastructure adapter is responsible for
 * annotating its implementation with {@code @WithSession}/{@code @WithTransaction}).
 */
public interface RestaurantRepository {

    // ------------------------------------------------------------------
    // Restaurant CRUD
    // ------------------------------------------------------------------

    /**
     * Persists a new restaurant (id assigned by the database/ID generator).
     *
     * @return a Uni emitting the persisted restaurant, with its id filled in
     */
    Uni<Restaurant> createRestaurant(Restaurant restaurant);

    /**
     * Updates an already-persisted restaurant.
     *
     * @return a Uni emitting the updated restaurant
     */
    Uni<Restaurant> updateRestaurant(Restaurant restaurant);

    /**
     * Finds a restaurant by its primary key.
     *
     * @return a Uni emitting an empty Optional if no restaurant has that id
     */
    Uni<Optional<Restaurant>> findRestaurantById(Long id);

    /**
     * Finds a restaurant by its (case-insensitive) display name.
     *
     * @return a Uni emitting an empty Optional if no restaurant matches that name
     */
    Uni<Optional<Restaurant>> findRestaurantByName(String name);

    /**
     * Returns all restaurants, sorted by name.
     */
    Uni<List<Restaurant>> findAllRestaurants();

    /**
     * Deletes a restaurant by id. The menu, menu items and schedule rows that belong
     * to the restaurant are removed as well (cascading clean-up done by the adapter).
     *
     * @return a Uni emitting {@code true} if a restaurant was deleted, {@code false} if none had that id
     */
    Uni<Boolean> deleteRestaurantById(Long id);

    // ------------------------------------------------------------------
    // Menu / MenuItem
    // ------------------------------------------------------------------

    /**
     * Creates the menu owned by a restaurant (one menu per restaurant).
     *
     * @return a Uni emitting the persisted menu, with its id filled in
     */
    Uni<Menu> createMenu(Menu menu);

    /**
     * Returns the menu of a restaurant, if it has one.
     */
    Uni<Optional<Menu>> findMenuByRestaurantId(Long restaurantId);

    /**
     * Adds an item (meal + price + available quantity) to a menu.
     *
     * @return a Uni emitting the persisted menu item, with its id filled in
     */
    Uni<MenuItem> createMenuItem(MenuItem menuItem);

    /**
     * Updates an existing menu item (e.g. re-pricing a meal).
     *
     * @return a Uni emitting the updated menu item
     */
    Uni<MenuItem> updateMenuItem(MenuItem menuItem);

    /**
     * Returns every item offered on the menu of a given restaurant.
     */
    Uni<List<MenuItem>> findMenuItemsByRestaurantId(Long restaurantId);

    /**
     * Atomically decrements the sellable quantity of a menu item (used when an order
     * is placed). The decrement only happens if enough stock is available.
     *
     * @return a Uni emitting the number of rows updated: 1 on success, 0 if the item
     *         does not exist or has less than {@code quantity} left
     */
    Uni<Integer> decrementMenuItemQuantity(Long menuItemId, int quantity);

    /**
     * Removes a menu item from a menu (e.g. a meal is no longer offered).
     *
     * @return a Uni emitting {@code true} if an item was deleted
     */
    Uni<Boolean> deleteMenuItemById(Long menuItemId);

    // ------------------------------------------------------------------
    // Meal catalogue
    // ------------------------------------------------------------------

    /**
     * Registers a new meal in the shared catalogue.
     *
     * @return a Uni emitting the persisted meal, with its id filled in
     */
    Uni<Meal> createMeal(Meal meal);

    /**
     * Finds a meal in the catalogue by its primary key.
     */
    Uni<Optional<Meal>> findMealById(Long id);

    /**
     * Returns all meals of the catalogue, sorted by name.
     */
    Uni<List<Meal>> findAllMeals();

    // ------------------------------------------------------------------
    // Schedule / OpeningPeriod
    // ------------------------------------------------------------------

    /**
     * Creates the schedule owned by a restaurant (one schedule per restaurant).
     *
     * @return a Uni emitting the persisted schedule, with its id filled in
     */
    Uni<Schedule> createSchedule(Schedule schedule);

    /**
     * Returns the schedule of a restaurant, if it has one.
     */
    Uni<Optional<Schedule>> findScheduleByRestaurantId(Long restaurantId);

    /**
     * Adds a weekly opening window to a schedule. Windows that cross midnight must be
     * split into two periods before being stored (two-row approach, see {@link OpeningPeriod}).
     *
     * @return a Uni emitting the persisted opening period, with its id filled in
     */
    Uni<OpeningPeriod> createOpeningPeriod(OpeningPeriod period);

    /**
     * Returns the weekly opening windows of a restaurant, sorted by day then opening time.
     */
    Uni<List<OpeningPeriod>> findOpeningPeriodsByRestaurantId(Long restaurantId);

    /**
     * Checks whether a restaurant is open at a given instant of its weekly schedule.
     *
     * @return a Uni emitting {@code true} if some opening period of that restaurant
     *         contains {@code time} on {@code day}
     */
    Uni<Boolean> isOpenAt(Long restaurantId, DayOfWeek day, LocalTime time);

    // ------------------------------------------------------------------
    // Test support
    // ------------------------------------------------------------------

    /**
     * Deletes every row handled by this repository. Intended for test clean-up between
     * test methods; production code should never call it.
     */
    Uni<Void> deleteAll();
}
