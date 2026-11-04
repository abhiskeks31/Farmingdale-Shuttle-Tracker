
-- Farmingdale Shuttle Tracker
-- Complete seven Nold Hall trip schedules
-- Stop 1 = Farmingdale LIRR
-- Stop 2 = Nold Hall
--
-- Nold Hall times are ESTIMATED arrivals,
-- not official published departures.

INSERT INTO public.schedule (
    schedule_id,
    route_id,
    stop_id,
    departure_time,
    day_of_week,
    trip_id,
    stop_order
)
SELECT
    nextval('public."Schedule_schedule_id_seq"'),
    1,
    v.stop_id,
    v.departure_time::time,
    'Mon-Fri',
    v.trip_id,
    v.stop_order
FROM (
    VALUES
        (1,  2, '07:15:00', 2),
        (2,  2, '07:30:00', 2),
        (3,  2, '07:47:00', 2),
        (5,  2, '08:32:00', 2),
        (6,  1, '08:35:00', 1),
        (6,  2, '08:45:00', 2),
        (7,  2, '09:05:00', 2),
        (17, 1, '11:50:00', 1),
        (17, 2, '12:00:00', 2)
) AS v(trip_id, stop_id, departure_time, stop_order)
JOIN public.trip AS t
    ON t.trip_id = v.trip_id
   AND t.route_id = 1
   AND t.final_destination = 'Nold Hall'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.schedule AS s
    WHERE s.trip_id = v.trip_id
      AND s.stop_order = v.stop_order
)
RETURNING
    schedule_id,
    trip_id,
    stop_id,
    stop_order,
    departure_time;
  