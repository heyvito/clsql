--;; up
CREATE TABLE dms
    (id INTEGER NOT NULL PRIMARY KEY,
     from_user_id INTEGER NOT NULL,
     to_user_id INTEGER NOT NULL,
     message TEXT NOT NULL);

--;; down
DROP TABLE dms;
