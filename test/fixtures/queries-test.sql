-- this is a random comment. It is in the beginning of the file.

--> a-simple-query
-- this is a docstring
-- it spans more than a line
-- and may take several others.
SELECT name, email, username
FROM users
WHERE email = :user-email
  AND name = :user-name

-- this is a random comment. It is in the middle of the file.
-- ah yiss, it is.

--> just-another-query
--@single-result
-- this is another docstring.
-- this query contains modifiers.
-- yes, it does.
SELECT name
FROM users
WHERE email = :user-email
LIMIT 1

-- this is a random comment.
-- It is in the end of the file.
