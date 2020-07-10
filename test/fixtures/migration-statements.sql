-- Hello, this is a migration file grammar test.
-- This migration creates a new foos table.
-- This is used by migrator_test.clj and query_execution_test.clj

--;; up
CREATE TYPE SAMPLE_ENUM AS ENUM ('FOO', 'BAR');
CREATE TABLE foos (
    id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT current_timestamp,
    kind SAMPLE_ENUM NOT NULL
);
CREATE INDEX foos_message ON foos (message);
CREATE INDEX foos_kind ON foos (kind);

--
-- This is another comment, between directions.
--

--;; down
DROP INDEX foos_message;
DROP INDEX foos_kind;
DROP TABLE foos;
DROP TYPE SAMPLE_ENUM;
