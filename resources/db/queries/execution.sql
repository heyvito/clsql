--> insert-no-return
INSERT INTO foos (message, kind)
     VALUES (:msg, :kind::SAMPLE_ENUM);

--> get-first
SELECT id, message, kind FROM foos LIMIT 1

--> get-bar
SELECT message, kind FROM foos WHERE kind = 'ABC'::SAMPLE_ENUM;

--> delete-by-id
DELETE FROM foos WHERE id = :id

--> insert-with-return
--@single-result
INSERT INTO foos (message, kind)
     VALUES (:msg, :kind::SAMPLE_ENUM)
  RETURNING id;
