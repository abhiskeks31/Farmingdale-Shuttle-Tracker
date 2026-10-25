import json
import urllib.request
import urllib.parse
from pathlib import Path

# Updated stop coordinates from Supabase
# Format: (latitude, longitude)
stops = {
    1: (40.73588158, -73.44175266),  # Farmingdale LIRR
    2: (40.749538, -73.4313365),      # Nold Hall
    3: (40.73005236, -73.42264232)   # Aviation Center
}

# Routes needed for our existing trips
route_segments = [
    (1, 2),  # LIRR -> Nold Hall
    (2, 3),  # Nold Hall -> Aviation Center
    (1, 3)   # LIRR -> Aviation Center
]

def generate_route(from_stop_id, to_stop_id):

    start_lat, start_lon = stops[from_stop_id]
    end_lat, end_lon = stops[to_stop_id]

    # OSRM requires longitude before latitude
    coordinates = (
        f"{start_lon},{start_lat};"
        f"{end_lon},{end_lat}"
    )

    url = (
            "https://router.project-osrm.org/route/v1/driving/"
            + coordinates
            + "?overview=full&geometries=geojson&steps=false"
    )

    request = urllib.request.Request(
        url,
        headers={"User-Agent": "FarmingdaleShuttleCapstone/1.0"}
    )

    with urllib.request.urlopen(request, timeout=30) as response:
        data = json.load(response)

    if data.get("code") != "Ok" or not data.get("routes"):
        raise RuntimeError(
            f"OSRM failed for {from_stop_id} -> {to_stop_id}: "
            f"{data.get('code')}"
        )

    route = data["routes"][0]

    # Convert OSRM [longitude, latitude] to [latitude, longitude]
    waypoints = [
        [latitude, longitude]
        for longitude, latitude
        in route["geometry"]["coordinates"]
    ]

    if len(waypoints) < 2:
        raise RuntimeError(
            f"Not enough waypoints for {from_stop_id} -> {to_stop_id}"
        )

    print(
        f"Route {from_stop_id} -> {to_stop_id}: "
        f"{len(waypoints)} waypoints, "
        f"{route['distance'] / 1000:.2f} km, "
        f"{route['duration'] / 60:.1f} minutes"
    )

    return {
        "fromStopId": from_stop_id,
        "toStopId": to_stop_id,
        "waypoints": waypoints
    }


def main():

    paths = []

    # Generate all required route segments
    for from_stop_id, to_stop_id in route_segments:
        paths.append(
            generate_route(from_stop_id, to_stop_id)
        )

    output = {"paths": paths}

    # Run this script from the shuttle-backend directory
    output_path = Path(
        "src/main/resources/simulation-routes.json"
    )

    output_path.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    # Replace the old JSON only after every route succeeds
    with output_path.open("w", encoding="utf-8") as file:
        json.dump(output, file, indent=2)

    print()
    print(f"Saved {len(paths)} routes to {output_path}")


if __name__ == "__main__":
    main()
