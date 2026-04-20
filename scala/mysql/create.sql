DROP TABLE likes;
DROP TABLE users;

CREATE TABLE IF NOT EXISTS users (id INT NOT NULL AUTO_INCREMENT,
                                  login VARCHAR (255) NOT NULL,
                                  password VARCHAR (255) NOT NULL,
                                  PRIMARY KEY (id));


INSERT INTO users (id, login, password)
VALUES
    (3, 'alice', '$2a$12$945DEyAEzxEm7pBb2YSnrukd1ccdC1UvElhbJyJpm8/8yfSRV2kMq'),
    (4, 'bob', '$2a$12$945DEyAEzxEm7pBb2YSnrukd1ccdC1UvElhbJyJpm8/8yfSRV2kMq'),
    (5, 'charlie', '$2a$12$945DEyAEzxEm7pBb2YSnrukd1ccdC1UvElhbJyJpm8/8yfSRV2kMq');


CREATE TABLE if NOT EXISTS likes (id INT NOT NULL AUTO_INCREMENT,
                                 user_id INT NOT NULL,      
                                 pokemon_id INT NOT NULL,
                                 PRIMARY KEY (id),
                                 FOREIGN KEY (user_id) REFERENCES users(id));                                 


INSERT INTO likes (user_id, pokemon_id)
VALUES
    (3, 6),
    (3, 25),
    (3, 12),
    (4, 182),
    (5, 1003),
    (5, 12);
