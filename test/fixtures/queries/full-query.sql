--> just-another-query
--@single-result
-- this is another docstring.
-- this query contains modifiers.
-- yes, it does.
SELECT name
FROM users
WHERE email = :user-email
LIMIT 1
