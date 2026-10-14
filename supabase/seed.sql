INSERT INTO "stop" (stop_id, stop_name, latitude, longitude)
VALUES 
  (1, 'farmingdale_lirr', 40.73588157993677, -73.44175266134374),
  (2, 'nold_hall', 40.73005235924392, -73.42264223233285),
  (3, 'aviation_center', 40.73005235924392, -73.42264223233285);

INSERT INTO route (route_id, route_name, status)
VALUES
    (1, 'lirr-nold', 'active'),
    (2, 'nold-lirr', 'active'),
    (3, 'lirr-aviation', 'active'),
    (4, 'aviation-lirr', 'active');

INSERT INTO route (route_id, route_name, status)
VALUES
    (1, 'farmingdale_shuttle', 'active');



INSERT INTO "driver" (driver_id, driver_name)
VALUES 
  (1, 'Tony Stark'),
  (2, 'Peter Parker'),
  (3, 'Steve Rogers');

INSERT INTO "shuttle" (shuttle_id, route_id, vehicle_number, bus_type, capacity, current_passengers, status, driver_id)
VALUES 
  (1, 1, 1, 'Shuttle', 30, 0, 'Active', 1),
  (2, 1, 2, 'Shuttle', 30, 0, 'Active', 2),
  (3, 1, 3, 'Shuttle', 30, 0, 'Active', 3);