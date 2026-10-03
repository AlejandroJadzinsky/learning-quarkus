package com.test.domain.repositories;

import com.test.domain.model.DayOfWeek;
import com.test.domain.model.Meal;
import com.test.domain.model.Menu;
import com.test.domain.model.MenuItem;
import com.test.domain.model.OpeningPeriod;
import com.test.domain.model.Restaurant;
import com.test.domain.model.Schedule;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests one nested class per group of {@link RestaurantRepository} operations, and one
 * test per operation (happy path + failure semantics). Runs against the deterministic
 * {@link InMemoryRestaurantRepository} double so it needs no database.
 */
class RestaurantRepositoryTest {

    private RestaurantRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRestaurantRepository();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Restaurant givenRestaurant(String name) {
        Restaurant r = new Restaurant();
        r.name = name;
        return Uni.createFrom().deferred(() -> repo.createRestaurant(r)).await().indefinitely();
    }

    private Meal givenMeal(String name) {
        Meal m = new Meal();
        m.name = name;
        return repo.createMeal(m).await().indefinitely();
    }

    private Menu givenMenu(Restaurant restaurant) {
        Menu menu = new Menu();
        menu.restaurantId = restaurant.id;
        return repo.createMenu(menu).await().indefinitely();
    }

    private Schedule givenSchedule(Restaurant restaurant) {
        Schedule s = new Schedule();
        s.restaurantId = restaurant.id;
        return repo.createSchedule(s).await().indefinitely();
    }

    private MenuItem givenMenuItem(Menu menu, Meal meal, String price, int quantity) {
        MenuItem item = new MenuItem();
        item.menu = menu;
        item.meal = meal;
        item.price = new BigDecimal(price);
        item.availableQuantity = quantity;
        return repo.createMenuItem(item).await().indefinitely();
    }

    private OpeningPeriod givenPeriod(Schedule schedule, DayOfWeek day, String open, String close) {
        OpeningPeriod p = new OpeningPeriod();
        p.schedule = schedule;
        p.day = day;
        p.openTime = LocalTime.parse(open);
        p.closeTime = LocalTime.parse(close);
        return repo.createOpeningPeriod(p).await().indefinitely();
    }

    // ------------------------------------------------------------------
    // Restaurant CRUD
    // ------------------------------------------------------------------

    @Nested
    class RestaurantOperations {

        @Test
        @DisplayName("createRestaurant assigns an id and persists the restaurant")
        void createRestaurant() {
            Restaurant created = givenRestaurant("Burger Palace");

            assertThat(created.id).isNotNull();
            assertThat(repo.findRestaurantById(created.id).await().indefinitely())
                    .get().hasFieldOrPropertyWithValue("name", "Burger Palace");
        }

        @Test
        @DisplayName("createRestaurant fails when the name is already taken (case-insensitive)")
        void createRestaurantRejectsDuplicateName() {
            givenRestaurant("Burger Palace");

            Throwable failure = catchThrowable(
                    () -> givenRestaurant("burger palace"));

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("updateRestaurant changes the stored restaurant")
        void updateRestaurant() {
            Restaurant resto = givenRestaurant("Burger Place");
            resto.name = "Burger Palace";

            Restaurant updated = repo.updateRestaurant(resto).await().indefinitely();

            assertThat(updated.name).isEqualTo("Burger Palace");
            assertThat(repo.findRestaurantById(resto.id).await().indefinitely())
                    .get().hasFieldOrPropertyWithValue("name", "Burger Palace");
        }

        @Test
        @DisplayName("updateRestaurant fails for an unknown id")
        void updateRestaurantUnknownId() {
            Restaurant ghost = new Restaurant();
            ghost.id = 99_999L;
            ghost.name = "Ghost Diner";

            Throwable failure = catchThrowable(() -> repo.updateRestaurant(ghost).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No restaurant with id");
        }

        @Test
        @DisplayName("findRestaurantById returns empty for an unknown id")
        void findRestaurantByIdUnknown() {
            assertThat(repo.findRestaurantById(1234L).await().indefinitely()).isEmpty();
        }

        @Test
        @DisplayName("findRestaurantByName is case-insensitive and empty for unknown names")
        void findRestaurantByName() {
            givenRestaurant("Green Bowl");

            assertThat(repo.findRestaurantByName("green bowl").await().indefinitely())
                    .get().hasFieldOrPropertyWithValue("name", "Green Bowl");
            assertThat(repo.findRestaurantByName("Pizza Hut").await().indefinitely()).isEmpty();
        }

        @Test
        @DisplayName("findAllRestaurants returns every restaurant sorted by name")
        void findAllRestaurants() {
            givenRestaurant("Tacos Loco");
            givenRestaurant("Burger Palace");
            givenRestaurant("green bowl");

            List<Restaurant> all = repo.findAllRestaurants().await().indefinitely();

            assertThat(all).extracting(r -> r.name)
                    .containsExactly("Burger Palace", "green bowl", "Tacos Loco");
        }

        @Test
        @DisplayName("deleteRestaurantById removes the restaurant and cascades to menu/items/schedule/periods")
        void deleteRestaurantByIdCascades() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Menu menu = givenMenu(resto);
            MenuItem item = givenMenuItem(menu, givenMeal("hamburger w/bacon and cheese"), "8.50", 60);
            Schedule schedule = givenSchedule(resto);
            givenPeriod(schedule, DayOfWeek.MONDAY, "11:00", "14:30");

            Boolean deleted = repo.deleteRestaurantById(resto.id).await().indefinitely();

            assertThat(deleted).isTrue();
            assertThat(repo.findRestaurantById(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findMenuByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findMenuItemsByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findScheduleByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findOpeningPeriodsByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findMealById(item.meal.id).await().indefinitely()).isPresent(); // shared catalogue untouched
        }

        @Test
        @DisplayName("deleteRestaurantById returns false for an unknown id")
        void deleteRestaurantByIdUnknown() {
            assertThat(repo.deleteRestaurantById(999L).await().indefinitely()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Menu / MenuItem
    // ------------------------------------------------------------------

    @Nested
    class MenuOperations {

        @Test
        @DisplayName("createMenu assigns an id and links it to its restaurant")
        void createMenu() {
            Restaurant resto = givenRestaurant("Burger Palace");

            Menu menu = givenMenu(resto);

            assertThat(menu.id).isNotNull();
            assertThat(repo.findMenuByRestaurantId(resto.id).await().indefinitely())
                    .get().extracting(m -> m.id).isEqualTo(menu.id);
        }

        @Test
        @DisplayName("createMenu enforces one menu per restaurant")
        void createMenuEnforcesUniqueness() {
            Restaurant resto = givenRestaurant("Burger Palace");
            givenMenu(resto);

            Throwable failure = catchThrowable(() -> {
                Menu second = new Menu();
                second.restaurantId = resto.id;
                repo.createMenu(second).await().indefinitely();
            });

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already has a menu");
        }

        @Test
        @DisplayName("createMenu fails when the restaurant does not exist")
        void createMenuUnknownRestaurant() {
            Menu orphan = new Menu();
            orphan.restaurantId = 4242L;

            Throwable failure = catchThrowable(() -> repo.createMenu(orphan).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unknown restaurant");
        }

        @Test
        @DisplayName("findMenuByRestaurantId returns empty when there is no menu yet")
        void findMenuByRestaurantIdEmpty() {
            Restaurant resto = givenRestaurant("Green Bowl");

            assertThat(repo.findMenuByRestaurantId(resto.id).await().indefinitely()).isEmpty();
        }

        @Test
        @DisplayName("createMenuItem stores price and quantity and can be read back through the restaurant")
        void createMenuItem() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Menu menu = givenMenu(resto);
            Meal burger = givenMeal("hamburger w/bacon and cheese");

            MenuItem item = givenMenuItem(menu, burger, "8.50", 60);

            assertThat(item.id).isNotNull();
            List<MenuItem> items = repo.findMenuItemsByRestaurantId(resto.id).await().indefinitely();
            assertThat(items).hasSize(1);
            assertThat(items.get(0).price).isEqualByComparingTo("8.50");
            assertThat(items.get(0).availableQuantity).isEqualTo(60);
            assertThat(items.get(0).meal.name).isEqualTo("hamburger w/bacon and cheese");
        }

        @Test
        @DisplayName("createMenuItem fails without a menu or a meal reference")
        void createMenuItemWithoutReferences() {
            MenuItem incomplete = new MenuItem();
            incomplete.price = BigDecimal.ONE;

            Throwable failure = catchThrowable(() -> repo.createMenuItem(incomplete).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("menu and a meal");
        }

        @Test
        @DisplayName("updateMenuItem re-prices an existing item")
        void updateMenuItem() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Menu menu = givenMenu(resto);
            MenuItem item = givenMenuItem(menu, givenMeal("fries (large)"), "3.00", 120);

            item.price = new BigDecimal("3.50");
            item.availableQuantity = 100;
            MenuItem updated = repo.updateMenuItem(item).await().indefinitely();

            assertThat(updated.price).isEqualByComparingTo("3.50");
            assertThat(updated.availableQuantity).isEqualTo(100);
        }

        @Test
        @DisplayName("updateMenuItem fails for an unknown id")
        void updateMenuItemUnknownId() {
            MenuItem ghost = new MenuItem();
            ghost.id = 777L;

            Throwable failure = catchThrowable(() -> repo.updateMenuItem(ghost).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No menu item with id");
        }

        @Test
        @DisplayName("findMenuItemsByRestaurantId only returns the items of that restaurant")
        void findMenuItemsByRestaurantIdFilters() {
            Restaurant bp = givenRestaurant("Burger Palace");
            Restaurant gb = givenRestaurant("Green Bowl");
            givenMenuItem(givenMenu(bp), givenMeal("hamburger w/bacon and cheese"), "8.50", 60);
            givenMenuItem(givenMenu(gb), givenMeal("veggie wrap"), "7.00", 40);

            List<MenuItem> bpItems = repo.findMenuItemsByRestaurantId(bp.id).await().indefinitely();

            assertThat(bpItems).hasSize(1);
            assertThat(bpItems.get(0).meal.name).isEqualTo("hamburger w/bacon and cheese");
        }

        @Test
        @DisplayName("decrementMenuItemQuantity subtracts stock when enough is available")
        void decrementQuantityHappyPath() {
            Restaurant resto = givenRestaurant("Burger Palace");
            MenuItem item = givenMenuItem(givenMenu(resto), givenMeal("fries (large)"), "3.00", 60);

            Integer updated = repo.decrementMenuItemQuantity(item.id, 15).await().indefinitely();

            assertThat(updated).isEqualTo(1);
            assertThat(item.availableQuantity).isEqualTo(45);
        }

        @Test
        @DisplayName("decrementMenuItemQuantity updates nothing when stock is insufficient")
        void decrementQuantityInsufficientStock() {
            Restaurant resto = givenRestaurant("Burger Palace");
            MenuItem item = givenMenuItem(givenMenu(resto), givenMeal("fries (large)"), "3.00", 10);

            Integer updated = repo.decrementMenuItemQuantity(item.id, 11).await().indefinitely();

            assertThat(updated).isZero();
            assertThat(item.availableQuantity).isEqualTo(10);
        }

        @Test
        @DisplayName("decrementMenuItemQuantity rejects non-positive quantities")
        void decrementQuantityInvalidArgument() {
            Restaurant resto = givenRestaurant("Burger Palace");
            MenuItem item = givenMenuItem(givenMenu(resto), givenMeal("fries (large)"), "3.00", 10);

            Throwable failure = catchThrowable(
                    () -> repo.decrementMenuItemQuantity(item.id, 0).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be positive");
        }

        @Test
        @DisplayName("deleteMenuItemById removes the item from the menu")
        void deleteMenuItemById() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Menu menu = givenMenu(resto);
            MenuItem item = givenMenuItem(menu, givenMeal("fries (large)"), "3.00", 10);

            Boolean deleted = repo.deleteMenuItemById(item.id).await().indefinitely();

            assertThat(deleted).isTrue();
            assertThat(repo.findMenuItemsByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.deleteMenuItemById(item.id).await().indefinitely()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Meal catalogue
    // ------------------------------------------------------------------

    @Nested
    class MealOperations {

        @Test
        @DisplayName("createMeal registers a meal in the shared catalogue")
        void createMeal() {
            Meal meal = givenMeal("tacos al pastor");

            assertThat(meal.id).isNotNull();
            assertThat(repo.findMealById(meal.id).await().indefinitely())
                    .get().hasFieldOrPropertyWithValue("name", "tacos al pastor");
        }

        @Test
        @DisplayName("createMeal rejects duplicate names")
        void createMealRejectsDuplicate() {
            givenMeal("churros");

            Throwable failure = catchThrowable(() -> givenMeal("churros"));

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("findMealById returns empty for an unknown id")
        void findMealByIdUnknown() {
            assertThat(repo.findMealById(313L).await().indefinitely()).isEmpty();
        }

        @Test
        @DisplayName("findAllMeals returns the whole catalogue sorted by name")
        void findAllMeals() {
            givenMeal("quinoa buddha bowl");
            givenMeal("fries (large)");
            givenMeal("veggie wrap");

            List<Meal> all = repo.findAllMeals().await().indefinitely();

            assertThat(all).extracting(m -> m.name)
                    .containsExactly("fries (large)", "quinoa buddha bowl", "veggie wrap");
        }
    }

    // ------------------------------------------------------------------
    // Schedule / OpeningPeriod
    // ------------------------------------------------------------------

    @Nested
    class ScheduleOperations {

        @Test
        @DisplayName("createSchedule assigns an id and links it to its restaurant")
        void createSchedule() {
            Restaurant resto = givenRestaurant("Tacos Loco");

            Schedule schedule = givenSchedule(resto);

            assertThat(schedule.id).isNotNull();
            assertThat(repo.findScheduleByRestaurantId(resto.id).await().indefinitely())
                    .get().extracting(s -> s.id).isEqualTo(schedule.id);
        }

        @Test
        @DisplayName("createSchedule enforces one schedule per restaurant")
        void createScheduleEnforcesUniqueness() {
            Restaurant resto = givenRestaurant("Tacos Loco");
            givenSchedule(resto);

            Throwable failure = catchThrowable(() -> {
                Schedule second = new Schedule();
                second.restaurantId = resto.id;
                repo.createSchedule(second).await().indefinitely();
            });

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already has a schedule");
        }

        @Test
        @DisplayName("createSchedule fails when the restaurant does not exist")
        void createScheduleUnknownRestaurant() {
            Schedule orphan = new Schedule();
            orphan.restaurantId = 99L;

            Throwable failure = catchThrowable(() -> repo.createSchedule(orphan).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("unknown restaurant");
        }

        @Test
        @DisplayName("findScheduleByRestaurantId returns empty when there is no schedule yet")
        void findScheduleByRestaurantIdEmpty() {
            Restaurant resto = givenRestaurant("Tacos Loco");

            assertThat(repo.findScheduleByRestaurantId(resto.id).await().indefinitely()).isEmpty();
        }

        @Test
        @DisplayName("createOpeningPeriod stores a weekly window")
        void createOpeningPeriod() {
            Schedule schedule = givenSchedule(givenRestaurant("Green Bowl"));

            OpeningPeriod period = givenPeriod(schedule, DayOfWeek.SATURDAY, "12:00", "16:00");

            assertThat(period.id).isNotNull();
            assertThat(repo.findOpeningPeriodsByRestaurantId(schedule.restaurantId).await().indefinitely())
                    .singleElement()
                    .extracting(p -> p.day).isEqualTo(DayOfWeek.SATURDAY);
        }

        @Test
        @DisplayName("createOpeningPeriod rejects windows that end before they start (split at midnight instead)")
        void createOpeningPeriodRejectsInvertedWindow() {
            Schedule schedule = givenSchedule(givenRestaurant("Tacos Loco"));
            OpeningPeriod crossed = new OpeningPeriod();
            crossed.schedule = schedule;
            crossed.day = DayOfWeek.FRIDAY;
            crossed.openTime = LocalTime.parse("23:00");
            crossed.closeTime = LocalTime.parse("01:00");

            Throwable failure = catchThrowable(() -> repo.createOpeningPeriod(crossed).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot end before it starts");
        }

        @Test
        @DisplayName("createOpeningPeriod requires an existing schedule")
        void createOpeningPeriodUnknownSchedule() {
            OpeningPeriod orphan = new OpeningPeriod();
            Schedule dangling = new Schedule();
            dangling.id = 55_555L;
            orphan.schedule = dangling;
            orphan.day = DayOfWeek.MONDAY;
            orphan.openTime = LocalTime.MIDNIGHT;
            orphan.closeTime = LocalTime.NOON;

            Throwable failure = catchThrowable(() -> repo.createOpeningPeriod(orphan).await().indefinitely());

            assertThat(failure).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("existing schedule");
        }

        @Test
        @DisplayName("findOpeningPeriodsByRestaurantId sorts periods by day then opening time")
        void findOpeningPeriodsSorted() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Schedule schedule = givenSchedule(resto);
            givenPeriod(schedule, DayOfWeek.MONDAY, "18:00", "22:00");
            givenPeriod(schedule, DayOfWeek.MONDAY, "11:00", "14:30");
            givenPeriod(schedule, DayOfWeek.FRIDAY, "11:00", "14:30");
            // another restaurant must not leak into this result
            givenPeriod(givenSchedule(givenRestaurant("Green Bowl")), DayOfWeek.TUESDAY, "11:30", "15:00");

            List<OpeningPeriod> periods = repo.findOpeningPeriodsByRestaurantId(resto.id).await().indefinitely();

            assertThat(periods).hasSize(3);
            assertThat(periods.get(0).openTime).isEqualTo(LocalTime.parse("11:00"));
            assertThat(periods.get(1).openTime).isEqualTo(LocalTime.parse("18:00"));
            assertThat(periods.get(2).day).isEqualTo(DayOfWeek.FRIDAY);
        }

        @Test
        @DisplayName("isOpenAt is true inside a window (inclusive bounds) and false outside")
        void isOpenAt() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Schedule schedule = givenSchedule(resto);
            givenPeriod(schedule, DayOfWeek.WEDNESDAY, "11:00", "14:30");

            assertThat(repo.isOpenAt(resto.id, DayOfWeek.WEDNESDAY, LocalTime.parse("12:15")).await().indefinitely()).isTrue();
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.WEDNESDAY, LocalTime.parse("11:00")).await().indefinitely()).isTrue();  // lower bound inclusive
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.WEDNESDAY, LocalTime.parse("14:30")).await().indefinitely()).isTrue();  // upper bound inclusive
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.WEDNESDAY, LocalTime.parse("14:31")).await().indefinitely()).isFalse();
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.SUNDAY, LocalTime.parse("12:00")).await().indefinitely()).isFalse();
        }

        @Test
        @DisplayName("isOpenAt handles the two-row split of a midnight-crossing window")
        void isOpenAtAcrossMidnight() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Schedule schedule = givenSchedule(resto);
            // Saturday 12:00 -> Sunday 01:00 stored as TWO rows (two-row approach)
            givenPeriod(schedule, DayOfWeek.SATURDAY, "12:00", "23:59:59");
            givenPeriod(schedule, DayOfWeek.SUNDAY, "00:00", "01:00");

            assertThat(repo.isOpenAt(resto.id, DayOfWeek.SATURDAY, LocalTime.parse("23:30")).await().indefinitely()).isTrue();
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.SUNDAY, LocalTime.parse("00:30")).await().indefinitely()).isTrue();
            assertThat(repo.isOpenAt(resto.id, DayOfWeek.SUNDAY, LocalTime.parse("01:01")).await().indefinitely()).isFalse();
        }

        @Test
        @DisplayName("isOpenAt returns false for an unknown restaurant")
        void isOpenAtUnknownRestaurant() {
            assertThat(repo.isOpenAt(12_345L, DayOfWeek.MONDAY, LocalTime.NOON).await().indefinitely()).isFalse();
        }
    }

    // ------------------------------------------------------------------
    // Test support
    // ------------------------------------------------------------------

    @Nested
    class Housekeeping {

        @Test
        @DisplayName("deleteAll wipes every entity managed by the repository")
        void deleteAll() {
            Restaurant resto = givenRestaurant("Burger Palace");
            Menu menu = givenMenu(resto);
            givenMenuItem(menu, givenMeal("hamburger w/bacon and cheese"), "8.50", 60);
            givenPeriod(givenSchedule(resto), DayOfWeek.MONDAY, "11:00", "14:30");

            repo.deleteAll().await().indefinitely();

            assertThat(repo.findAllRestaurants().await().indefinitely()).isEmpty();
            assertThat(repo.findAllMeals().await().indefinitely()).isEmpty();
            assertThat(repo.findMenuByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findScheduleByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findMenuItemsByRestaurantId(resto.id).await().indefinitely()).isEmpty();
            assertThat(repo.findOpeningPeriodsByRestaurantId(resto.id).await().indefinitely()).isEmpty();
        }
    }

    // ------------------------------------------------------------------
    // End-to-end scenario across all entities
    // ------------------------------------------------------------------

    @Test
    @DisplayName("full setup: restaurant + menu + items + schedule behaves consistently")
    void fullRestaurantSetup() {
        Restaurant tacosLoco = givenRestaurant("Tacos Loco");
        Menu menu = givenMenu(tacosLoco);
        Meal tacos = givenMeal("tacos al pastor");
        Meal churros = givenMeal("churros");
        MenuItem tacoItem = givenMenuItem(menu, tacos, "2.50", 200);
        givenMenuItem(menu, churros, "4.00", 50);
        Schedule schedule = givenSchedule(tacosLoco);
        givenPeriod(schedule, DayOfWeek.FRIDAY, "19:00", "23:59:59");

        // an order of 3 tacos comes in on Friday night
        assertThat(repo.isOpenAt(tacosLoco.id, DayOfWeek.FRIDAY, LocalTime.parse("21:00")).await().indefinitely()).isTrue();
        assertThat(repo.decrementMenuItemQuantity(tacoItem.id, 3).await().indefinitely()).isEqualTo(1);

        Optional<Restaurant> found = repo.findRestaurantByName("tacos loco").await().indefinitely();
        assertThat(found).isPresent();
        assertThat(repo.findMenuItemsByRestaurantId(found.get().id).await().indefinitely()).hasSize(2);
        assertThat(tacoItem.availableQuantity).isEqualTo(197);
    }
}
