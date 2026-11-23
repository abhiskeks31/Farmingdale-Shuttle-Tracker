BEGIN;

DO $$
DECLARE
    midday_trip_id BIGINT;
    afternoon_trip_id BIGINT;
BEGIN

    -- ============================================
    -- Trip 1: Nold Hall -> Farmingdale LIRR
    -- ============================================

    INSERT INTO public.trip (
        route_id,
        final_destination,
        service_days
    )
    VALUES (
        1,
        'Farmingdale LIRR',
        'Mon-Fri'
    )
    RETURNING trip_id INTO midday_trip_id;

    INSERT INTO public.schedule (
        route_id,
        stop_id,
        departure_time,
        day_of_week,
        trip_id,
        stop_order
    )
    VALUES
        (1, 2, '12:20:00', 'Mon-Fri', midday_trip_id, 1),
        (1, 1, '12:30:00', 'Mon-Fri', midday_trip_id, 2);


    -- ============================================
    -- Trip 2: Aviation -> Nold Hall -> LIRR
    -- ============================================

    INSERT INTO public.trip (
        route_id,
        final_destination,
        service_days
    )
    VALUES (
        1,
        'Farmingdale LIRR',
        'Mon-Fri'
    )
    RETURNING trip_id INTO afternoon_trip_id;

    INSERT INTO public.schedule (
        route_id,
        stop_id,
        departure_time,
        day_of_week,
        trip_id,
        stop_order
    )
    VALUES
        (1, 3, '16:15:00', 'Mon-Fri', afternoon_trip_id, 1),
        (1, 2, '16:25:00', 'Mon-Fri', afternoon_trip_id, 2),
        (1, 1, '16:35:00', 'Mon-Fri', afternoon_trip_id, 3);

    RAISE NOTICE 'Created midday return trip: %', midday_trip_id;
    RAISE NOTICE 'Created afternoon return trip: %', afternoon_trip_id;

END $$;

COMMIT;
