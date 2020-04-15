--;; up
CREATE TABLE tweets
    (id INTEGER NOT NULL PRIMARY KEY,
     user_id INTEGER NOT NULL,
     post TEXT NOT NULL);

--;; down
DROP TABLE tweets;
