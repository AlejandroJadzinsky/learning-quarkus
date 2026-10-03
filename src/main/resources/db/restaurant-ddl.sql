-- ============================================================================
-- DDL for the meal-delivery domain model (com.test.domain.model)
-- MySQL 8+ / MariaDB compatible. Column names match the Hibernate Reactive
-- Panache entities so this script can create the schema by itself
-- (quarkus.hibernate-orm.schema-management.strategy=none) or be used as a
-- reference for DBAs in production (%prod uses strategy=validate).
--
-- It also seeds a couple of sample restaurant setups:
--   * Burger Palace  : lunch + dinner windows, splits Saturday midnight into
--                      two OpeningPeriod rows (two-row approach)
--   * Green Bowl     : vegetarian spot, open all week at lunch
--   * Tacos Loco     : late-night tacos, dinner only
-- ============================================================================

CREATE DATABASE IF NOT EXISTS quarkus
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE quarkus;

-- ---------------------------------------------------------------------------
-- Schema
-- ---------------------------------------------------------------------------

DROP TABLE IF EXISTS MenuItem;
DROP TABLE IF EXISTS Menu;
DROP TABLE IF EXISTS OpeningPeriod;
DROP TABLE IF EXISTS Schedule;
DROP TABLE IF EXISTS Meal;
DROP TABLE IF EXISTS Restaurant;

-- A restaurant in the meal delivery system
CREATE TABLE Restaurant (
    id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_restaurant_name UNIQUE (name)
);

-- Shared catalogue of meals (many restaurants can offer the same meal)
CREATE TABLE Meal (
    id   BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uq_meal_name UNIQUE (name)
);

-- The menu owned by exactly one restaurant
CREATE TABLE Menu (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    restaurantId BIGINT NOT NULL,
    CONSTRAINT uq_menu_restaurant UNIQUE (restaurantId),
    CONSTRAINT fk_menu_restaurant FOREIGN KEY (restaurantId)
        REFERENCES Restaurant (id) ON DELETE CASCADE
);

-- One entry of a menu: meal + price + remaining sellable quantity
CREATE TABLE MenuItem (
    id                BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    menu_id           BIGINT       NOT NULL,
    meal_id           BIGINT       NOT NULL,
    price             DECIMAL(10, 2) NOT NULL,
    availableQuantity INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_menuitem_menu FOREIGN KEY (menu_id)
        REFERENCES Menu (id) ON DELETE CASCADE,
    CONSTRAINT fk_menuitem_meal FOREIGN KEY (meal_id)
        REFERENCES Meal (id) ON DELETE CASCADE,
    CONSTRAINT ck_menuitem_quantity CHECK (availableQuantity >= 0),
    CONSTRAINT ck_menuitem_price CHECK (price >= 0)
);

-- The weekly activity schedule owned by exactly one restaurant
CREATE TABLE Schedule (
    id           BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    restaurantId BIGINT NOT NULL,
    CONSTRAINT uq_schedule_restaurant UNIQUE (restaurantId),
    CONSTRAINT fk_schedule_restaurant FOREIGN KEY (restaurantId)
        REFERENCES Restaurant (id) ON DELETE CASCADE
);

-- One weekly opening window; midnight-crossing windows are stored as TWO rows
-- so that openTime <= closeTime always holds within a row
CREATE TABLE OpeningPeriod (
    id        BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT    NOT NULL,
    day       VARCHAR(10) NOT NULL,
    openTime  TIME        NOT NULL,
    closeTime TIME        NOT NULL,
    CONSTRAINT ck_opening_period_order CHECK (openTime <= closeTime),
    CONSTRAINT fk_period_schedule FOREIGN KEY (schedule_id)
        REFERENCES Schedule (id) ON DELETE CASCADE
);

CREATE INDEX ix_menuitem_menu ON MenuItem (menu_id);
CREATE INDEX ix_menuitem_meal ON MenuItem (meal_id);
CREATE INDEX ix_period_schedule_day ON OpeningPeriod (schedule_id, day);

-- ---------------------------------------------------------------------------
-- Sample data: three restaurant setups
-- ---------------------------------------------------------------------------

INSERT INTO Restaurant (id, name) VALUES
    (1, 'Burger Palace'),
    (2, 'Green Bowl'),
    (3, 'Tacos Loco');

INSERT INTO Meal (id, name) VALUES
    (1, 'hamburger w/bacon and cheese'),
    (2, 'fries (large)'),
    (3, 'veggie wrap'),
    (4, 'quinoa buddha bowl'),
    (5, 'tacos al pastor'),
    (6, 'churros');

-- Menus: one per restaurant
INSERT INTO Menu (id, restaurantId) VALUES
    (1, 1),
    (2, 2),
    (3, 3);

-- Menu items: per-restaurant price and stock
INSERT INTO MenuItem (id, menu_id, meal_id, price, availableQuantity) VALUES
    (1, 1, 1, 8.50,  60),   -- Burger Palace sells the classic burger
    (2, 1, 2, 3.00,  120),  -- ...with large fries
    (3, 2, 3, 7.00,  40),   -- Green Bowl: veggie wrap
    (4, 2, 4, 9.50,  35),   -- Green Bowl: buddha bowl
    (5, 3, 5, 2.50,  200),  -- Tacos Loco: tacos al pastor
    (6, 3, 6, 4.00,  50),   -- Tacos Loco: churros
    (7, 3, 2, 3.50,  80);   -- Tacos Loco also does (pricier) large fries

-- Schedules: one per restaurant
INSERT INTO Schedule (id, restaurantId) VALUES
    (1, 1),
    (2, 2),
    (3, 3);

-- Opening periods (weekly windows, inclusive bounds)
INSERT INTO OpeningPeriod (id, schedule_id, day, openTime, closeTime) VALUES
    -- Burger Palace: Mon-Fri lunch, Mon-Sat dinner,
    -- Sat night spills over midnight -> split into TWO rows (two-row approach)
    (1,  1, 'MONDAY',    '11:00:00', '14:30:00'),
    (2,  1, 'MONDAY',    '18:00:00', '22:00:00'),
    (3,  1, 'TUESDAY',   '11:00:00', '14:30:00'),
    (4,  1, 'TUESDAY',   '18:00:00', '22:00:00'),
    (5,  1, 'WEDNESDAY', '11:00:00', '14:30:00'),
    (6,  1, 'WEDNESDAY', '18:00:00', '22:00:00'),
    (7,  1, 'THURSDAY',  '11:00:00', '14:30:00'),
    (8,  1, 'THURSDAY',  '18:00:00', '22:00:00'),
    (9,  1, 'FRIDAY',    '11:00:00', '14:30:00'),
    (10, 1, 'FRIDAY',    '18:00:00', '23:00:00'),
    (11, 1, 'SATURDAY',  '12:00:00', '23:59:59'), -- first half of the cross-midnight window
    (12, 1, 'SUNDAY',    '00:00:00', '01:00:00'), -- second half (spillover)
    -- Green Bowl: every day at lunch, closed evenings
    (13, 2, 'MONDAY',    '11:30:00', '15:00:00'),
    (14, 2, 'TUESDAY',   '11:30:00', '15:00:00'),
    (15, 2, 'WEDNESDAY', '11:30:00', '15:00:00'),
    (16, 2, 'THURSDAY',  '11:30:00', '15:00:00'),
    (17, 2, 'FRIDAY',    '11:30:00', '15:00:00'),
    (18, 2, 'SATURDAY',  '12:00:00', '16:00:00'),
    -- Tacos Loco: late-night only, Tue-Sun (closed Monday)
    (19, 3, 'TUESDAY',   '19:00:00', '23:59:59'),
    (20, 3, 'WEDNESDAY', '19:00:00', '23:59:59'),
    (21, 3, 'THURSDAY',  '19:00:00', '23:59:59'),
    (22, 3, 'FRIDAY',    '19:00:00', '23:59:59'),
    (23, 3, 'SATURDAY',  '19:00:00', '23:59:59'),
    (24, 3, 'SUNDAY',    '19:00:00', '23:00:00');
