package com.test.infrastructure.addapters;

import com.test.domain.model.DayOfWeek;
import com.test.domain.model.Meal;
import com.test.domain.model.Menu;
import com.test.domain.model.MenuItem;
import com.test.domain.model.OpeningPeriod;
import com.test.domain.model.Restaurant;
import com.test.domain.model.Schedule;
import com.test.domain.repositories.RestaurantRepository;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Hibernate Reactive / Panache adapter implementing the {@link RestaurantRepository} port.
 * <p>
 * Read operations run in a session ({@code @WithSession}); write operations run inside a
 * transaction so that bulk updates/deletes and cascading clean-ups are atomic.
 */
@ApplicationScoped
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    // ------------------------------------------------------------------
    // Restaurant CRUD
    // ------------------------------------------------------------------

    @Override
    @WithTransaction
    public Uni<Restaurant> createRestaurant(Restaurant restaurant) {
        return Restaurant.<Restaurant>find("name", restaurant.name)
                .firstResult()
                .onItem().ifNotNull().failWith(
                        () -> new IllegalStateException("A restaurant named '" + restaurant.name + "' already exists"))
                .onItem().ifNull().continueWith(restaurant)
                .chain(r -> r.<Restaurant>persist());
    }

    @Override
    @WithTransaction
    public Uni<Restaurant> updateRestaurant(Restaurant restaurant) {
        return Restaurant.<Restaurant>findById(restaurant.id)
                .onItem().ifNull().failWith(() -> new IllegalStateException("No restaurant with id " + restaurant.id))
                .onItem().ifNotNull().call(existing -> {
                    existing.name = restaurant.name;
                    return Panache.withTransaction(existing::persist);
                });
    }

    @Override
    @WithSession
    public Uni<Optional<Restaurant>> findRestaurantById(Long id) {
        return Restaurant.<Restaurant>findById(id).onItem().transform(Optional::ofNullable);
    }

    @Override
    @WithSession
    public Uni<Optional<Restaurant>> findRestaurantByName(String name) {
        return Restaurant.<Restaurant>find("lower(name)", name.toLowerCase())
                .firstResult()
                .onItem().transform(Optional::ofNullable);
    }

    @Override
    @WithSession
    public Uni<List<Restaurant>> findAllRestaurants() {
        return Restaurant.listAll(Sort.by("name"));
    }

    @Override
    @WithTransaction
    public Uni<Boolean> deleteRestaurantById(Long id) {
        // cascade clean-up of everything owned by this restaurant
        Uni<Long> menuItems = MenuItem.delete("menu.restaurantId", id);
        Uni<Long> menu = Menu.delete("restaurantId", id);
        Uni<Long> periods = OpeningPeriod.delete("schedule.restaurantId", id);
        Uni<Long> schedule = Schedule.delete("restaurantId", id);
        return menuItems
                .chain(() -> menu)
                .chain(() -> periods)
                .chain(() -> schedule)
                .chain(() -> Restaurant.deleteById(id));
    }

    // ------------------------------------------------------------------
    // Menu / MenuItem
    // ------------------------------------------------------------------

    @Override
    @WithTransaction
    public Uni<Menu> createMenu(Menu menu) {
        return Menu.<Menu>find("restaurantId", menu.restaurantId)
                .firstResult()
                .onItem().ifNotNull().failWith(
                        () -> new IllegalStateException("Restaurant " + menu.restaurantId + " already has a menu"))
                .onItem().ifNull().continueWith(menu)
                .chain(m -> m.<Menu>persist());
    }

    @Override
    @WithSession
    public Uni<Optional<Menu>> findMenuByRestaurantId(Long restaurantId) {
        return Menu.<Menu>find("restaurantId", restaurantId)
                .firstResult()
                .onItem().transform(Optional::ofNullable);
    }

    @Override
    @WithTransaction
    public Uni<MenuItem> createMenuItem(MenuItem menuItem) {
        return menuItem.<MenuItem>persist();
    }

    @Override
    @WithTransaction
    public Uni<MenuItem> updateMenuItem(MenuItem menuItem) {
        return MenuItem.<MenuItem>findById(menuItem.id)
                .onItem().ifNull().failWith(() -> new IllegalStateException("No menu item with id " + menuItem.id))
                .onItem().ifNotNull().call(existing -> {
                    existing.price = menuItem.price;
                    existing.availableQuantity = menuItem.availableQuantity;
                    return Panache.withTransaction(existing::persist);
                });
    }

    @Override
    @WithSession
    public Uni<List<MenuItem>> findMenuItemsByRestaurantId(Long restaurantId) {
        return listAndFetch(MenuItem.<MenuItem>find("menu.restaurantId", restaurantId));
    }

    @Override
    @WithTransaction
    public Uni<Integer> decrementMenuItemQuantity(Long menuItemId, int quantity) {
        if (quantity <= 0) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Quantity to decrement must be positive, got " + quantity));
        }
        return MenuItem.update(
                "availableQuantity = availableQuantity - ?1 where id = ?2 and availableQuantity >= ?1",
                quantity, menuItemId);
    }

    @Override
    @WithTransaction
    public Uni<Boolean> deleteMenuItemById(Long menuItemId) {
        return MenuItem.deleteById(menuItemId);
    }

    // ------------------------------------------------------------------
    // Meal catalogue
    // ------------------------------------------------------------------

    @Override
    @WithTransaction
    public Uni<Meal> createMeal(Meal meal) {
        return Meal.<Meal>find("name", meal.name)
                .firstResult()
                .onItem().ifNotNull().failWith(
                        () -> new IllegalStateException("A meal named '" + meal.name + "' already exists"))
                .onItem().ifNull().continueWith(meal)
                .chain(m -> m.<Meal>persist());
    }

    @Override
    @WithSession
    public Uni<Optional<Meal>> findMealById(Long id) {
        return Meal.<Meal>findById(id).onItem().transform(Optional::ofNullable);
    }

    @Override
    @WithSession
    public Uni<List<Meal>> findAllMeals() {
        return Meal.listAll(Sort.by("name"));
    }

    // ------------------------------------------------------------------
    // Schedule / OpeningPeriod
    // ------------------------------------------------------------------

    @Override
    @WithTransaction
    public Uni<Schedule> createSchedule(Schedule schedule) {
        return Schedule.<Schedule>find("restaurantId", schedule.restaurantId)
                .firstResult()
                .onItem().ifNotNull().failWith(
                        () -> new IllegalStateException("Restaurant " + schedule.restaurantId + " already has a schedule"))
                .onItem().ifNull().continueWith(schedule)
                .chain(s -> s.<Schedule>persist());
    }

    @Override
    @WithSession
    public Uni<Optional<Schedule>> findScheduleByRestaurantId(Long restaurantId) {
        return Schedule.<Schedule>find("restaurantId", restaurantId)
                .firstResult()
                .onItem().transform(Optional::ofNullable);
    }

    @Override
    @WithTransaction
    public Uni<OpeningPeriod> createOpeningPeriod(OpeningPeriod period) {
        if (period.openTime.isAfter(period.closeTime)) {
            // invariant open <= close per row: caller must split midnight-crossing windows
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "An opening period cannot end before it starts (split midnight-crossing windows into two periods): "
                            + period.openTime + " > " + period.closeTime));
        }
        return period.<OpeningPeriod>persist();
    }

    @Override
    @WithSession
    public Uni<List<OpeningPeriod>> findOpeningPeriodsByRestaurantId(Long restaurantId) {
        return listAndFetch(OpeningPeriod.<OpeningPeriod>find("schedule.restaurantId",
                Sort.by("day").and("openTime"), restaurantId));
    }

    @Override
    @WithSession
    public Uni<Boolean> isOpenAt(Long restaurantId, DayOfWeek day, LocalTime time) {
        return OpeningPeriod.count(
                "schedule.restaurantId = ?1 and day = ?2 and openTime <= ?3 and closeTime >= ?3",
                restaurantId, day, time)
                .map(count -> count > 0);
    }

    // ------------------------------------------------------------------
    // Test support
    // ------------------------------------------------------------------

    @Override
    @WithTransaction
    public Uni<Void> deleteAll() {
        return MenuItem.deleteAll()
                .chain(() -> Menu.deleteAll())
                .chain(() -> OpeningPeriod.deleteAll())
                .chain(() -> Schedule.deleteAll())
                .chain(() -> Meal.deleteAll())
                .chain(() -> Restaurant.deleteAll())
                .replaceWithVoid();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Materialises a lazy {@link PanacheQuery} into a fully fetched list
     * (fetch-joined children would otherwise be detached).
     */
    private static <T> Uni<List<T>> listAndFetch(PanacheQuery<T> query) {
        return query.list();
    }
}
