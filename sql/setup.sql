-- StreamVault MySQL Setup Script
-- Run this entire file in MySQL Workbench on a fresh database.
-- ⚠ ADMIN LOGIN: After running this, register an account via /register.html,
--   then promote it:  UPDATE Users SET role = 'admin' WHERE email = 'your@email.com';

CREATE DATABASE IF NOT EXISTS streamvault;
USE streamvault;

-- -----------------------------------------------
-- TABLES
-- -----------------------------------------------

CREATE TABLE IF NOT EXISTS Subscription_Plans (
    plan_id       INT AUTO_INCREMENT PRIMARY KEY,
    plan_name     VARCHAR(50)    NOT NULL UNIQUE,
    monthly_price DECIMAL(10,2) NOT NULL CHECK (monthly_price > 0)
);

CREATE TABLE IF NOT EXISTS Users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(100)  UNIQUE NOT NULL,
    password      VARCHAR(255),
    country       VARCHAR(50),
    date_of_birth DATE,
    join_date     DATE          DEFAULT (CURRENT_DATE),
    is_active     BOOLEAN       DEFAULT TRUE,
    role          VARCHAR(20)   DEFAULT 'viewer'
                                CHECK (role IN ('viewer','content_manager','admin'))
);

CREATE TABLE IF NOT EXISTS Subscriptions (
    subscription_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT          NOT NULL,
    plan_id         INT          NOT NULL,
    status          VARCHAR(20)  DEFAULT 'active'
                                 CHECK (status IN ('active','inactive','cancelled')),
    start_date      DATE,
    auto_renew      BOOLEAN      DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES Users(user_id),
    FOREIGN KEY (plan_id) REFERENCES Subscription_Plans(plan_id)
);

CREATE TABLE IF NOT EXISTS Payments (
    payment_id      INT AUTO_INCREMENT PRIMARY KEY,
    subscription_id INT,
    amount          DECIMAL(10,2) CHECK (amount >= 0),
    payment_date    DATE DEFAULT (CURRENT_DATE),
    status          VARCHAR(20)  DEFAULT 'completed'
                                 CHECK (status IN ('completed','pending','failed')),
    FOREIGN KEY (subscription_id) REFERENCES Subscriptions(subscription_id)
);

CREATE TABLE IF NOT EXISTS Studios (
    studio_id   INT AUTO_INCREMENT PRIMARY KEY,
    studio_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS Genres (
    genre_id   INT AUTO_INCREMENT PRIMARY KEY,
    genre_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Languages (
    language_id   INT AUTO_INCREMENT PRIMARY KEY,
    language_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Content_Items (
    content_id       INT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200)  NOT NULL,
    type             VARCHAR(50)   CHECK (type IN ('Movie','Series','Music','Book')),
    release_year     INT           CHECK (release_year BETWEEN 1888 AND 2100),
    age_rating       VARCHAR(10),
    average_rating   DECIMAL(3,1)  DEFAULT 0.0 CHECK (average_rating BETWEEN 0 AND 10),
    total_reviews    INT           DEFAULT 0 CHECK (total_reviews >= 0),
    duration_minutes INT,
    synopsis         TEXT,
    studio_id        INT,
    FOREIGN KEY (studio_id) REFERENCES Studios(studio_id)
);

-- M:N junction: Content <-> Genre
CREATE TABLE IF NOT EXISTS Content_Genres (
    content_id INT NOT NULL,
    genre_id   INT NOT NULL,
    PRIMARY KEY (content_id, genre_id),
    FOREIGN KEY (content_id) REFERENCES Content_Items(content_id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id)   REFERENCES Genres(genre_id)
);

-- M:N junction: Content <-> Language
CREATE TABLE IF NOT EXISTS Content_Languages (
    content_id  INT NOT NULL,
    language_id INT NOT NULL,
    PRIMARY KEY (content_id, language_id),
    FOREIGN KEY (content_id)  REFERENCES Content_Items(content_id) ON DELETE CASCADE,
    FOREIGN KEY (language_id) REFERENCES Languages(language_id)
);

CREATE TABLE IF NOT EXISTS Reviews (
    review_id   INT AUTO_INCREMENT PRIMARY KEY,
    content_id  INT NOT NULL,
    user_id     INT NOT NULL,
    rating      DECIMAL(3,1) CHECK (rating BETWEEN 1 AND 10),
    review_text TEXT,
    review_date DATE DEFAULT (CURRENT_DATE),
    FOREIGN KEY (content_id) REFERENCES Content_Items(content_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES Users(user_id)
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
    progress_pct INT          DEFAULT 0 CHECK (progress_pct BETWEEN 0 AND 100),
    completed    BOOLEAN      DEFAULT FALSE,
    FOREIGN KEY (user_id)    REFERENCES Users(user_id),
    FOREIGN KEY (content_id) REFERENCES Content_Items(content_id)
);

-- -----------------------------------------------
-- INDEXES
-- -----------------------------------------------

CREATE INDEX IF NOT EXISTS idx_content_type_year   ON Content_Items(type, release_year);
CREATE INDEX IF NOT EXISTS idx_watch_user_date     ON Watch_History(user_id, watch_date);
CREATE INDEX IF NOT EXISTS idx_subscription_status ON Subscriptions(user_id, status);
CREATE FULLTEXT INDEX IF NOT EXISTS ft_content_title ON Content_Items(title);

-- -----------------------------------------------
-- REFERENCE DATA
-- -----------------------------------------------

INSERT IGNORE INTO Subscription_Plans (plan_name, monthly_price) VALUES
('Basic',    7.99),
('Standard', 13.99),
('Premium',  17.99);

INSERT IGNORE INTO Studios (studio_name) VALUES
('Warner Bros.'),
('Netflix Studios'),
('AMC Networks'),
('HBO'),
('Universal');

INSERT IGNORE INTO Genres (genre_name) VALUES
('Thriller'),
('Crime'),
('Drama'),
('Action'),
('Comedy'),
('Sci-Fi'),
('Horror'),
('Fantasy'),
('History');

INSERT IGNORE INTO Languages (language_name) VALUES
('English'),
('Spanish'),
('French'),
('Arabic');

-- -----------------------------------------------
-- CONTENT
-- -----------------------------------------------

INSERT IGNORE INTO Content_Items
    (content_id, title, type, release_year, age_rating, average_rating, total_reviews, duration_minutes, synopsis, studio_id)
VALUES
(1,  'Breaking Bad',     'Series', 2008, 'R',      9.5, 2, NULL, 'A chemistry teacher gets diagnosed with cancer and starts cooking meth with one of his old students to make money for his family. Things go very wrong very fast.', 3),
(2,  'The Dark Knight',  'Movie',  2008, 'PG-13',  9.0, 3,  152, 'Batman teams up with a DA and the police to clean up Gotham, but the Joker shows up and ruins everything. Widely considered one of the best superhero movies ever made.', 1),
(3,  'Stranger Things',  'Series', 2016, 'TV-14',  8.7, 2, NULL, 'A kid goes missing in a small town and his friends start uncovering some seriously weird stuff — secret government labs, monsters from another dimension, and a strange girl with powers.', 2),
(4,  'Inception',        'Movie',  2010, 'PG-13',  8.8, 2,  148, 'A guy who can enter people''s dreams is hired to plant an idea in someone''s head instead of stealing one. The deeper into the dream they go the more complicated it gets.', 1),
(5,  'The Crown',        'Series', 2016, 'TV-MA',  8.6, 1, NULL, 'Follows Queen Elizabeth II from when she first took the throne and shows all the political drama and personal struggles behind the scenes of the royal family over the decades.', 4),
(6,  'Interstellar',     'Movie',  2014, 'PG-13',  8.6, 2,  169, 'Earth is basically dying so a group of astronauts go through a wormhole to find a new planet for humanity to live on. Gets very heavy into time and space stuff.', 1),
(7,  'Better Call Saul', 'Series', 2015, 'TV-MA',  9.0, 1, NULL, 'Prequel to Breaking Bad. Follows Jimmy McGill, a small-time lawyer who slowly turns into the shady Saul Goodman we see in BB. Actually better than people expect.', 3),
(8,  'Avengers Endgame', 'Movie',  2019, 'PG-13',  8.4, 3,  181, 'The follow up to Infinity War. The remaining Avengers come up with a plan to undo what Thanos did. Big finale to over 10 years of Marvel movies.', 5),
(9,  'Ozark',            'Series', 2017, 'TV-MA',  8.5, 1, NULL, 'A financial advisor gets caught up with a drug cartel and moves his whole family to the Ozarks to launder money and keep them all alive. Very stressful to watch.', 2),
(10, 'The Witcher',      'Series', 2019, 'TV-MA',  8.2, 2, NULL, 'Based on a book series. Follows a monster hunter called Geralt in a dark fantasy world. The timeline jumps around a lot but it gets easier to follow after a few episodes.', 2);

-- Content_Genres (content_id, genre_id)
INSERT IGNORE INTO Content_Genres VALUES
(1,1),(1,2),    -- Breaking Bad:     Thriller, Crime
(2,4),(2,2),    -- The Dark Knight:  Action, Crime
(3,7),(3,6),    -- Stranger Things:  Horror, Sci-Fi
(4,6),(4,1),    -- Inception:        Sci-Fi, Thriller
(5,3),(5,9),    -- The Crown:        Drama, History
(6,6),(6,3),    -- Interstellar:     Sci-Fi, Drama
(7,3),(7,2),    -- Better Call Saul: Drama, Crime
(8,4),(8,6),    -- Avengers Endgame: Action, Sci-Fi
(9,1),(9,3),    -- Ozark:            Thriller, Drama
(10,8),(10,4);  -- The Witcher:      Fantasy, Action

-- Content_Languages (content_id, language_id)
INSERT IGNORE INTO Content_Languages VALUES
(1,1),(2,1),(3,1),(4,1),(4,3),(5,1),(6,1),(7,1),(7,2),(8,1),(9,1),(10,1);

-- Episodes
INSERT IGNORE INTO Episodes (content_id, season_no, episode_no, title, duration_minutes) VALUES
(1, 1, 1, 'Pilot',                        58),
(1, 1, 2, 'Cat''s in the Bag',            48),
(1, 2, 1, 'Seven Thirty-Seven',           47),
(3, 1, 1, 'The Vanishing of Will Byers',  47),
(3, 1, 2, 'The Weirdo on Maple Street',   55),
(3, 2, 1, 'MADMAX',                       48),
(7, 1, 1, 'Uno',                          46),
(7, 1, 2, 'Mijo',                         50),
(9, 1, 1, 'Sugarwood',                    60),
(9, 1, 2, 'Casino Nation',                58),
(10,1, 1, 'The End''s Beginning',         61),
(10,1, 2, 'Four Marks',                   59);

-- -----------------------------------------------
-- DEMO USERS
-- Note: these users have placeholder passwords and cannot log in.
-- Register a real account via /register.html to access the application.
-- To make your account an admin, run:
--   UPDATE Users SET role = 'admin' WHERE email = 'your@email.com';
-- -----------------------------------------------

-- user_id 1..6 (auto-assigned on fresh DB)
INSERT IGNORE INTO Users (full_name, email, password, country, date_of_birth, join_date, is_active, role) VALUES
('Admin User',    'admin@streamvault.com', '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'UAE', NULL,         '2024-01-01', TRUE,  'admin'),
('Layla Al-Farsi','layla@streamvault.com', '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'UAE', '1995-03-12', '2024-01-15', TRUE,  'viewer'),
('James Carter',  'james@example.com',     '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'USA', '1990-07-22', '2024-02-10', TRUE,  'viewer'),
('Sara Khalid',   'sara@example.com',      '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'UK',  '1998-11-05', '2024-03-20', TRUE,  'viewer'),
('Tom Nguyen',    'tom@example.com',       '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'AUS', '1985-04-18', '2024-04-01', FALSE, 'viewer'),
('Mia Russo',     'mia@example.com',       '$2a$12$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', 'ITA', '2000-09-30', CURDATE(),    TRUE,  'viewer');

-- -----------------------------------------------
-- SUBSCRIPTIONS  (user_ids 1-6, plan_ids 1-3)
-- -----------------------------------------------

INSERT IGNORE INTO Subscriptions (user_id, plan_id, status, start_date, auto_renew) VALUES
(1, 3, 'active',    '2024-01-01', TRUE),   -- admin:  Premium  $17.99
(2, 1, 'active',    '2024-01-15', TRUE),   -- layla:  Basic    $7.99
(3, 2, 'active',    '2024-02-10', TRUE),   -- james:  Standard $13.99
(4, 3, 'active',    '2024-03-20', TRUE),   -- sara:   Premium  $17.99
(5, 1, 'cancelled', '2024-04-01', FALSE),  -- tom:    Basic    (cancelled)
(6, 2, 'active',    CURDATE(),    TRUE);   -- mia:    Standard $13.99  ← new this month

-- -----------------------------------------------
-- PAYMENTS  (amounts match each plan's price)
-- subscription_id auto-assigned 1..6 matching Subscriptions rows above
-- -----------------------------------------------

INSERT IGNORE INTO Payments (subscription_id, amount, payment_date, status) VALUES
-- Admin Premium (sub 1, $17.99) — historical
(1, 17.99, '2024-01-01', 'completed'),
(1, 17.99, '2024-02-01', 'completed'),
(1, 17.99, '2024-03-01', 'completed'),
(1, 17.99, '2024-04-01', 'completed'),
-- Layla Basic (sub 2, $7.99) — historical
(2,  7.99, '2024-01-15', 'completed'),
(2,  7.99, '2024-02-15', 'completed'),
(2,  7.99, '2024-03-15', 'completed'),
(2,  7.99, '2024-04-15', 'completed'),
-- James Standard (sub 3, $13.99) — historical
(3, 13.99, '2024-02-10', 'completed'),
(3, 13.99, '2024-03-10', 'completed'),
(3, 13.99, '2024-04-10', 'completed'),
-- Sara Premium (sub 4, $17.99) — historical
(4, 17.99, '2024-03-20', 'completed'),
(4, 17.99, '2024-04-20', 'completed'),
-- Tom Basic (sub 5, $7.99) — one payment before cancellation
(5,  7.99, '2024-04-01', 'completed'),
-- Current-month payments (will appear in "Revenue This Month" report)
(1, 17.99, CURDATE(), 'completed'),
(2,  7.99, CURDATE(), 'completed'),
(3, 13.99, CURDATE(), 'completed'),
(4, 17.99, CURDATE(), 'completed'),
(6, 13.99, CURDATE(), 'completed');

-- -----------------------------------------------
-- WATCH HISTORY  (gives content for dashboard + admin analytics)
-- -----------------------------------------------

INSERT IGNORE INTO Watch_History (user_id, content_id, watch_date, progress_pct, completed) VALUES
-- Layla (user 2)
(2,  1, DATE_SUB(CURDATE(), INTERVAL 2  DAY),  45, FALSE),
(2,  2, DATE_SUB(CURDATE(), INTERVAL 8  DAY), 100, TRUE),
(2,  3, DATE_SUB(CURDATE(), INTERVAL 14 DAY),  60, FALSE),
(2,  6, DATE_SUB(CURDATE(), INTERVAL 21 DAY), 100, TRUE),
-- James (user 3)
(3,  2, DATE_SUB(CURDATE(), INTERVAL 3  DAY), 100, TRUE),
(3,  4, DATE_SUB(CURDATE(), INTERVAL 9  DAY),  70, FALSE),
(3,  8, DATE_SUB(CURDATE(), INTERVAL 18 DAY), 100, TRUE),
(3,  1, DATE_SUB(CURDATE(), INTERVAL 25 DAY),  80, FALSE),
-- Sara (user 4)
(4,  5, DATE_SUB(CURDATE(), INTERVAL 4  DAY), 100, TRUE),
(4, 10, DATE_SUB(CURDATE(), INTERVAL 11 DAY), 100, TRUE),
(4,  7, DATE_SUB(CURDATE(), INTERVAL 17 DAY),  35, FALSE),
(4,  9, DATE_SUB(CURDATE(), INTERVAL 28 DAY), 100, TRUE),
-- Admin (user 1)
(1,  2, DATE_SUB(CURDATE(), INTERVAL 1  DAY), 100, TRUE),
(1,  6, DATE_SUB(CURDATE(), INTERVAL 5  DAY), 100, TRUE),
-- Mia (user 6)
(6,  8, DATE_SUB(CURDATE(), INTERVAL 1  DAY),  55, FALSE),
(6,  3, DATE_SUB(CURDATE(), INTERVAL 6  DAY), 100, TRUE);

-- -----------------------------------------------
-- REVIEWS  (user_id references demo users 1-4)
-- -----------------------------------------------

INSERT IGNORE INTO Reviews (content_id, user_id, rating, review_text, review_date) VALUES
(1, 2, 9.5, 'honestly one of the best shows ive ever watched. Walter White''s transformation is insane to follow season by season', DATE_SUB(CURDATE(), INTERVAL 10 DAY)),
(1, 3, 9.0, 'started it cause everyone kept recommending it and now i get why. took 2 episodes to get hooked but after that i couldnt stop', DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
(2, 2, 9.0, 'heath ledger as the joker is on another level. rewatched this 3 times already', DATE_SUB(CURDATE(), INTERVAL 8 DAY)),
(2, 3, 9.5, 'best superhero movie ever made no debate. the interrogation scene alone is worth a 10', DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
(2, 4, 8.5, 'really good but its hard to top the first one. still better than most action movies out there', DATE_SUB(CURDATE(), INTERVAL 12 DAY)),
(3, 2, 8.5, 'so nostalgic, reminds me of 80s movies i grew up watching. the kids are surprisingly good actors', DATE_SUB(CURDATE(), INTERVAL 15 DAY)),
(3, 4, 9.0, 'season 1 was perfect. got a bit messy in later seasons but still worth watching', DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(4, 2, 8.5, 'you need to pay attention the whole time but its worth it. had to pause and think a few times', DATE_SUB(CURDATE(), INTERVAL 9 DAY)),
(4, 4, 9.0, 'nolan really does something special with this one. the ending still confuses me but in a good way', DATE_SUB(CURDATE(), INTERVAL 4 DAY)),
(5, 4, 8.5, 'not usually into historical stuff but this was actually really interesting. production quality is insane', DATE_SUB(CURDATE(), INTERVAL 20 DAY)),
(6, 2, 9.0, 'cried at the end not gonna lie. the father daughter story hits different once you understand the time stuff', DATE_SUB(CURDATE(), INTERVAL 22 DAY)),
(6, 3, 8.0, 'visually stunning and the story is interesting but the ending is a bit confusing. still a solid watch', DATE_SUB(CURDATE(), INTERVAL 19 DAY)),
(7, 4, 9.0, 'people sleep on this show. its slow at first but jimmy mcgill is one of the best written characters on tv', DATE_SUB(CURDATE(), INTERVAL 16 DAY)),
(8, 2, 8.0, 'emotional sendoff for a lot of characters. a few plot holes if you think too hard but overall really satisfying', DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
(8, 3, 9.0, 'the final battle gave me chills. 10 years of buildup paid off', DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
(8, 4, 8.0, 'good ending but felt a bit rushed in some parts. still cried though', DATE_SUB(CURDATE(), INTERVAL 11 DAY)),
(9, 4, 8.5, 'super underrated. marty and wendy are both terrible people and i love watching them', DATE_SUB(CURDATE(), INTERVAL 29 DAY)),
(10, 2, 8.0, 'the timeline jumping around in season 1 is confusing at first but stick with it. gets much better', DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(10, 3, 8.5, 'geralt is a great character. wish they kept the original cast for season 3 though', DATE_SUB(CURDATE(), INTERVAL 6 DAY));
