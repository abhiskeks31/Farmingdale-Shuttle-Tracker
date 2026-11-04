DELETE FROM public.trip AS t
WHERE t.trip_id IN (31, 33, 40, 51)
  AND t.final_destination IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM public.schedule AS s
      WHERE s.trip_id = t.trip_id
  )
RETURNING t.trip_id;