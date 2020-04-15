-- This migration creates a new Users table.

--;; up
CREATE TABLE users
    (id INTEGER NOT NULL PRIMARY KEY,
     name VARCHAR NOT NULL, -- We're allowing comments to be here as well.
     email TEXT NOT NULL UNIQUE);

--
-- This is another comment, between directions.
--

--;; down
DROP TABLE users;
