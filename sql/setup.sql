-- StreamVault MySQL Setup Script
-- Run this entire file in MySQL Workbench

CREATE DATABASE IF NOT EXISTS streamvault;
USE streamvault;

-- -----------------------------------------------
-- TABLES
-- -----------------------------------------------

CREATE TABLE IF NOT EXISTS Subscription_Plans (
    plan_id       INT AUTO_INCREMENT PRIMARY KEY,
    plan_name     VARCHAR(50)    NOT NULL,
    monthly_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS Users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(100)  UNIQUE NOT NULL,
    password      VARCHAR(255),
    country       VARCHAR(50),
    date_of_birth DATE,
    join_date     DATE          DEFAULT (CURRENT_DATE),
    is_active     BOOLEAN       DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS Subscriptions (
    subscription_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT,
    plan_id         INT,
    status          VARCHAR(20)  DEFAULT 'active',
    start_date      DATE,
    auto_renew      BOOLEAN      DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (plan_id) REFERENCES Subscription_Plans(plan_id)
);

CREATE TABLE IF NOT EXISTS Payments (
    payment_id       INT AUTO_INCREMENT PRIMARY KEY,
    subscription_id  INT,
    amount           DECIMAL(10,2),
    payment_date     DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (subscription_id) REFERENCES Subscriptions(subscription_id)
);

CREATE TABLE IF NOT EXISTS Studios (
    studio_id   INT AUTO_INCREMENT PRIMARY KEY,
    studio_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS Genres (
    genre_id   INT AUTO_INCREMENT PRIMARY KEY,
    genre_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Languages (
    language_id   INT AUTO_INCREMENT PRIMARY KEY,
    language_name VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS Content_Items (
    content_id     INT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(200)  NOT NULL,
    type           VARCHAR(50),
    release_year   INT,
    age_rating     VARCHAR(10),
    average_rating DECIMAL(3,1) DEFAULT 0.0,
    total_reviews  INT          DEFAULT 0,
    studio_id      INT,
    FOREIGN KEY (studio_id) REFERENCES Studios(studio_id)
);

CREATE TABLE IF NOT EXISTS Episodes (
    episode_id       INT AUTO_INCREMENT PRIMARY KEY,
    content_id       INT,
    season_no        INT,
    episode_no       INT,
    title            VARCHAR(200),
    duration_minutes INT,
    FOREIGN KEY (content_id) REFERENCES Content_Items(content_id)
);

CREATE TABLE IF NOT EXISTS Watch_History (
    history_id   INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT,
    profile_id   INT          DEFAULT 1,
    content_id   INT,
    watch_date   DATE         DEFAULT (CURRENT_DATE),
    progress_pct INT          DEFAULT 0,
    completed    BOOLEAN      DEFAULT FALSE,
    FOREIGN KEY (user_id)    REFERENCES Users(user_id),
    FOREIGN KEY (content_id) REFERENCES Content_Items(content_id)
);

-- -----------------------------------------------
-- INDEXES (from Phase 3 Section 4.1)
-- -----------------------------------------------

CREATE INDEX idx_content_type_year
    ON Content_Items(type, release_year);

CREATE INDEX idx_watch_profile
    ON Watch_History(profile_id, watch_date);

CREATE INDEX idx_subscription_user_status
    ON Subscriptions(user_id, status);

CREATE FULLTEXT INDEX ft_content_title
    ON Content_Items(title);

-- -----------------------------------------------
-- SAMPLE DATA
-- -----------------------------------------------

INSERT INTO Subscription_Plans (plan_name, monthly_price) VALUES
('Basic',    7.99),
('Standard', 13.99),
('Premium',  17.99);

INSERT INTO Studios (studio_name) VALUES
('Warner Bros.'),
('Netflix Studios'),
('AMC Networks'),
('HBO'),
('Universal');

INSERT INTO Genres (genre_name) VALUES
('Thriller'),
('Crime'),
('Drama'),
('Action'),
('Comedy'),
('Sci-Fi'),
('Horror');

INSERT INTO Languages (language_name) VALUES
('English'),
('Spanish'),
('French'),
('Arabic');

INSERT INTO Content_Items (title, type, release_year, age_rating, average_rating, total_reviews, studio_id) VALUES
('Breaking Bad',         'Series', 2008, 'R',   9.5, 2, 3),
('The Dark Knight',      'Movie',  2008, 'PG-13', 9.0, 3, 1),
('Stranger Things',      'Series', 2016, 'TV-14', 8.7, 2, 2),
('Inception',            'Movie',  2010, 'PG-13', 8.8, 2, 1),
('The Crown',            'Series', 2016, 'TV-MA', 8.6, 1, 4),
('Interstellar',         'Movie',  2014, 'PG-13', 8.6, 2, 1),
('Better Call Saul',     'Series', 2015, 'TV-MA', 9.0, 1, 3),
('Avengers Endgame',     'Movie',  2019, 'PG-13', 8.4, 3, 5),
('Ozark',                'Series', 2017, 'TV-MA', 8.5, 1, 2),
('The Witcher',          'Series', 2019, 'TV-MA', 8.2, 2, 2);

INSERT INTO Episodes (content_id, season_no, episode_no, title, duration_minutes) VALUES
(1, 1, 1, 'Pilot',            58),
(1, 1, 2, 'Cat\'s in the Bag', 48),
(3, 1, 1, 'The Vanishing of Will Byers', 47),
(3, 1, 2, 'The Weirdo on Maple Street',  55),
(7, 1, 1, 'Uno',              46),
(7, 1, 2, 'Mijo',             50);

-- one test user (password is 'password123' hashed - you can update this after registering via the app)
INSERT INTO Users (full_name, email, password, country, date_of_birth, join_date, is_active) VALUES
('Layla Al-Farsi', 'layla.alfarsi@email.com', 'placeholder', 'UAE', '1995-03-12', '2024-01-15', TRUE);

INSERT INTO Subscriptions (user_id, plan_id, status, start_date, auto_renew) VALUES
(1, 3, 'active', '2024-01-15', TRUE);

INSERT INTO Payments (subscription_id, amount, payment_date) VALUES
(1, 17.99, '2024-01-15'),
(1, 17.99, '2024-02-15'),
(1, 17.99, '2024-03-15');

INSERT INTO Watch_History (user_id, profile_id, content_id, watch_date, progress_pct, completed) VALUES
(1, 1, 1, '2024-11-03', 80,  FALSE),
(1, 1, 2, '2024-11-05', 100, TRUE),
(1, 1, 3, '2024-11-10', 60,  FALSE);
