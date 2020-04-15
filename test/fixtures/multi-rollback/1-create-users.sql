--;; up
CREATE TABLE users
    (id INTEGER NOT NULL PRIMARY KEY,
     name VARCHAR NOT NULL,
     email TEXT NOT NULL UNIQUE);

--;; down
DROP TABLE users;
