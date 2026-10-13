ALTER TABLE public.schedule
ADD COLUMN trip_id BIGINT,
ADD COLUMN stop_order BIGINT;

ALTER TABLE public.schedule
ADD CONSTRAINT fk_schedule_trip
FOREIGN KEY (trip_id)
REFERENCES public.trip(trip_id);