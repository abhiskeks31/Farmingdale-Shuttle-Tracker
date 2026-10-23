import json
import urllib.request
from pathlib import Path

# Coordinates: longitude, latitude
start = (-73.44175266, 40.73588158)
end = (-73.42264232, 40.73005236)

url = (
    "https://router.project-osrm.org/route/v1/driving/"
    f"{start[0]},{start[1]};{end[0]},{end[1]}"
    "?overview=full&geometries=geojson&steps=false"
)

with urllib.request.urlopen(url, timeout=20) as response:
    data = json.load(response)

if data.get("code") != "Ok" or not data.get("routes"):
    raise RuntimeError(f"OSRM routing failed: {data.get('code')}")

route = data["routes"][0]

# Convert OSRM [longitude, latitude] to [latitude, longitude]
waypoints = [
    [lat, lon]
    for lon, lat in route["geometry"]["coordinates"]
]

output = {
    "paths": [
        {
            "fromStopId": 1,
            "toStopId": 2,
            "waypoints": waypoints
        }
    ]
}

output_path = Path("src/main/resources/simulation-routes.json")
output_path.parent.mkdir(parents=True, exist_ok=True)

with output_path.open("w", encoding="utf-8") as file:
    json.dump(output, file, indent=2)

print(f"Saved {len(waypoints)} waypoints to {output_path}")
print(f"Road distance: {route['distance'] / 1000:.2f} km")
print(f"Estimated driving time: {route['duration'] / 60:.1f} minutes")
