package com.test.domain.repositories;

import com.test.domain.model.Meal;
import com.test.domain.model.Menu;
import com.test.domain.model.MenuItem;
import com.test.domain.model.OpeningPeriod;
import com.test.domain.model.Restaurant;
import com.test.domain.model.Schedule;
import io.smallrye.mutiny.Uni;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Deterministic in-memory implementation of {@link RestaurantRepository} used to unit
 * test every repository operation without a database (the real Hibernate Reactive
 * adapter needs dev services/docker, which is not always available).
 * <p>
 * It faithfully reproduces the documented semantics of each operation: id generation,
 * uniqueness constraints, cascading deletion of a restaurant's menu/schedule, the
 * guarded quantity decrement, and the open-time check.
 */
public class InMemoryRestaurantRepository implements RestaurantRepository {

    private final AtomicLong ids = new AtomicLong(1);

    private final Map<Long, Restaurant> restaurants = new HashMap<>();
    private final Map<Long, Menu> menus = new HashMap<>();
    private final Map<Long, MenuItem> menuItems = new HashMap<>();
    private final Map<Long, Meal> meals = new HashMap<>();
    private final Map<Long, Schedule> schedules = new HashMap<>();
    private final Map<Long, OpeningPeriod> openingPeriods = new HashMap<>();

    // ------------------------------------------------------------------
    // Restaurant CRUD
    // ------------------------------------------------------------------

    @Override
    public Uni<Restaurant> createRestaurant(Restaurant restaurant) {
        if (restaurant.name == null || restaurant.name.isBlank()) {
            return Uni.createFrom().failure(new IllegalArgumentException("A restaurant needs a name"));
        }
        if (restaurants.values().stream()
                .anyMatch(r -> r.name.equalsIgnoreCase(restaurant.name))) {
            return Uni.createFrom().failure(
                    new IllegalStateException("A restaurant named '" + restaurant.name + "' already exists"));
        }
        restaurant.id = ids.getAndIncrement();
        restaurants.put(restaurant.id, restaurant);
        return Uni.createFrom().item(restaurant);
    }

    @Override
    public Uni<Restaurant> updateRestaurant(Restaurant restaurant) {
        if (restaurant.id == null || !restaurants.containsKey(restaurant.id)) {
            return Uni.createFrom().failure(
                    new IllegalStateException("No restaurant with id " + restaurant.id));
        }
        restaurants.put(restaurant.id, restaurant);
        return Uni.createFrom().item(restaurant);
    }

    @Override
    public Uni<Optional<Restaurant>> findRestaurantById(Long id) {
        return Uni.createFrom().item(Optional.ofNullable(restaurants.get(id)));
    }

    @Override
    public Uni<Optional<Restaurant>> findRestaurantByName(String name) {
        return Uni.createFrom().item(restaurants.values().stream()
                .filter(r -> r.name.equalsIgnoreCase(name))
                .findFirst());
    }

    @Override
    public Uni<List<Restaurant>> findAllRestaurants() {
        List<Restaurant> all = new ArrayList<>(restaurants.values());
        all.sort(Comparator.comparing(r -> r.name.toLowerCase()));
        return Uni.createFrom().item(all);
    }

    @Override
    public Uni<Boolean> deleteRestaurantById(Long id) {
        boolean removed = restaurants.remove(id) != null;
        if (removed) {
            // cascade clean-up, mirroring the adapter
            menus.values().removeIf(m -> id.equals(m.restaurantId));
            menuItems.values().removeIf(i -> i.menu == null || id.equals(i.menu.restaurantId));
            schedules.values().removeIf(s -> id.equals(s.restaurantId));
            openingPeriods.values().removeIf(p -> p.schedule == null || id.equals(p.schedule.restaurantId));
        }
        return Uni.createFrom().item(removed);
    }

    // ------------------------------------------------------------------
    // Menu / MenuItem
    // ------------------------------------------------------------------

    @Override
    public Uni<Menu> createMenu(Menu menu) {
        if (menu.restaurantId == null || !restaurants.containsKey(menu.restaurantId)) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Menu references unknown restaurant " + menu.restaurantId));
        }
        boolean hasMenu = menus.values().stream()
                .anyMatch(m -> m.restaurantId.equals(menu.restaurantId));
        if (hasMenu) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Restaurant " + menu.restaurantId + " already has a menu"));
        }
        menu.id = ids.getAndIncrement();
        menus.put(menu.id, menu);
        return Uni.createFrom().item(menu);
    }

    @Override
    public Uni<Optional<Menu>> findMenuByRestaurantId(Long restaurantId) {
        return Uni.createFrom().item(menus.values().stream()
                .filter(m -> m.restaurantId.equals(restaurantId))
                .findFirst());
    }

    @Override
    public Uni<MenuItem> createMenuItem(MenuItem menuItem) {
        if (menuItem.menu == null || menuItem.meal == null) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "A menu item must reference both a menu and a meal"));
        }
        menuItem.id = ids.getAndIncrement();
        menuItems.put(menuItem.id, menuItem);
        return Uni.createFrom().item(menuItem);
    }

    @Override
    public Uni<MenuItem> updateMenuItem(MenuItem menuItem) {
        if (menuItem.id == null || !menuItems.containsKey(menuItem.id)) {
            return Uni.createFrom().failure(
                    new IllegalStateException("No menu item with id " + menuItem.id));
        }
        MenuItem existing = menuItems.get(menuItem.id);
        existing.price = menuItem.price;
        existing.availableQuantity = menuItem.availableQuantity;
        return Uni.createFrom().item(existing);
    }

    @Override
    public Uni<List<MenuItem>> findMenuItemsByRestaurantId(Long restaurantId) {
        return Uni.createFrom().item(menuItems.values().stream()
                .filter(i -> i.menu != null && restaurantId.equals(i.menu.restaurantId))
                .sorted(Comparator.comparing(i -> i.id))
                .toList());
    }

    @Override
    public Uni<Integer> decrementMenuItemQuantity(Long menuItemId, int quantity) {
        if (quantity <= 0) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "Quantity to decrement must be positive, got " + quantity));
        }
        MenuItem item = menuItems.get(menuItemId);
        if (item == null || item.availableQuantity < quantity) {
            return Uni.createFrom().item(0);
        }
        item.availableQuantity -= quantity;
        return Uni.createFrom().item(1);
    }

    @Override
    public Uni<Boolean> deleteMenuItemById(Long menuItemId) {
        return Uni.createFrom().item(menuItems.remove(menuItemId) != null);
    }

    // ------------------------------------------------------------------
    // Meal catalogue
    // ------------------------------------------------------------------

    @Override
    public Uni<Meal> createMeal(Meal meal) {
        if (meals.values().stream().anyMatch(m -> m.name.equalsIgnoreCase(meal.name))) {
            return Uni.createFrom().failure(
                    new IllegalStateException("A meal named '" + meal.name + "' already exists"));
        }
        meal.id = ids.getAndIncrement();
        meals.put(meal.id, meal);
        return Uni.createFrom().item(meal);
    }

    @Override
    public Uni<Optional<Meal>> findMealById(Long id) {
        return Uni.createFrom().item(Optional.ofNullable(meals.get(id)));
    }

    @Override
    public Uni<List<Meal>> findAllMeals() {
        List<Meal> all = new ArrayList<>(meals.values());
        all.sort(Comparator.comparing(m -> m.name.toLowerCase()));
        return Uni.createFrom().item(all);
    }

    // ------------------------------------------------------------------
    // Schedule / OpeningPeriod
    // ------------------------------------------------------------------

    @Override
    public Uni<Schedule> createSchedule(Schedule schedule) {
        if (schedule.restaurantId == null || !restaurants.containsKey(schedule.restaurantId)) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Schedule references unknown restaurant " + schedule.restaurantId));
        }
        boolean hasSchedule = schedules.values().stream()
                .anyMatch(s -> s.restaurantId.equals(schedule.restaurantId));
        if (hasSchedule) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "Restaurant " + schedule.restaurantId + " already has a schedule"));
        }
        schedule.id = ids.getAndIncrement();
        schedules.put(schedule.id, schedule);
        return Uni.createFrom().item(schedule);
    }

    @Override
    public Uni<Optional<Schedule>> findScheduleByRestaurantId(Long restaurantId) {
        return Uni.createFrom().item(schedules.values().stream()
                .filter(s -> s.restaurantId.equals(restaurantId))
                .findFirst());
    }

    @Override
    public Uni<OpeningPeriod> createOpeningPeriod(OpeningPeriod period) {
        if (period.openTime.isAfter(period.closeTime)) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "An opening period cannot end before it starts (split midnight-crossing windows into two periods): "
                            + period.openTime + " > " + period.closeTime));
        }
        if (period.schedule == null || !schedules.containsKey(period.schedule.id)) {
            return Uni.createFrom().failure(new IllegalStateException(
                    "An opening period must belong to an existing schedule"));
        }
        period.id = ids.getAndIncrement();
        openingPeriods.put(period.id, period);
        return Uni.createFrom().item(period);
    }

    @Override
    public Uni<List<OpeningPeriod>> findOpeningPeriodsByRestaurantId(Long restaurantId) {
        return Uni.createFrom().item(openingPeriods.values().stream()
                .filter(p -> p.schedule != null && restaurantId.equals(p.schedule.restaurantId))
                .sorted(Comparator.comparing((OpeningPeriod p) -> p.day)
                        .thenComparing(p -> p.openTime))
                .toList());
    }

    @Override
    public Uni<Boolean> isOpenAt(Long restaurantId, com.test.domain.model.DayOfWeek day, LocalTime time) {
        return Uni.createFrom().item(openingPeriods.values().stream()
                .anyMatch(p -> p.schedule != null
                        && restaurantId.equals(p.schedule.restaurantId)
                        && p.day == day
                        && !time.isBefore(p.openTime)
                        && !time.isAfter(p.closeTime)));
    }

    // ------------------------------------------------------------------
    // Test support
    // ------------------------------------------------------------------

    @Override
    public Uni<Void> deleteAll() {
        menuItems.clear();
        menus.clear();
        openingPeriods.clear();
        schedules.clear();
        meals.clear();
        restaurants.clear();
        return Uni.createFrom().voidItem();
    }
}
