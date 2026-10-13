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