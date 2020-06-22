--> a-more-complex-query
-- Returns a list of foos
SELECT foo.id,
       foo.bar,
       foo.baz
FROM update
WHERE bar = :bar-kind
  AND baz > :random-baz
  AND (NOT disabled OR foo.id IN (SELECT id FROM buz WHERE baz = :other-placeholder))
  AND foo.id IN (SELECT id FROM buz WHERE baz = :another-baz)
ORDER BY bar DESC
