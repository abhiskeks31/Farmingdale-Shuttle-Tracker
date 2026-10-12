ALTER TABLE public."Stop"
RENAME TO stop;

ALTER TABLE public."Route"
RENAME TO route;

ALTER TABLE public."RouteStop"
RENAME TO route_stop;

ALTER TABLE public."Schedule"
RENAME TO schedule;

ALTER TABLE public."Shuttle"
RENAME TO shuttle;

ALTER TABLE public."GPSLocation"
RENAME TO gps_location;

ALTER TABLE public."Occupancy"
RENAME TO occupancy;

ALTER TABLE public."Driver"
RENAME TO driver;

ALTER TABLE public."DriverFeedback"
RENAME TO driver_feedback;