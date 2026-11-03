#!/usr/bin/env python3
"""Add Aviation Center (3) -> Nold Hall (2) to the existing four-route JSON.
"""
import argparse
import json
import math
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

FROM_ID, TO_ID = 3, 2
AVIATION = (40.73005235924392, -73.42264223233285)
NOLD = (40.749538, -73.4313365)
EXPECTED_PAIRS = {(1, 2), (2, 3), (1, 3), (2, 1)}


def meters(a, b):
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 6371000 * 2 * math.asin(min(1, math.sqrt(h)))


def fetch_route():
    # OSRM expects longitude,latitude; our JSON stores [latitude,longitude].
    coordinates = f'{AVIATION[1]},{AVIATION[0]};{NOLD[1]},{NOLD[0]}'
    params = urlencode({'overview': 'full', 'geometries': 'geojson', 'steps': 'false'})
    url = f'https://router.project-osrm.org/route/v1/driving/{coordinates}?{params}'
    req = Request(url, headers={'User-Agent': 'FarmingdaleShuttleTrackerCapstone/1.0'})
    with urlopen(req, timeout=30) as response:
        result = json.load(response)
    if result.get('code') != 'Ok' or not result.get('routes'):
        raise ValueError(f'OSRM error: {result.get("code")} {result.get("message", "")}')
    route = result['routes'][0]
    coordinates = route['geometry']['coordinates']
    if len(coordinates) < 2:
        raise ValueError('OSRM route has fewer than two waypoints')
    waypoints = [[round(lat, 6), round(lon, 6)] for lon, lat in coordinates]
    if meters(waypoints[0], AVIATION) > 150:
        raise ValueError('Route begins more than 150 meters from Aviation Center')
    if meters(waypoints[-1], NOLD) > 150:
        raise ValueError('Route ends more than 150 meters from Nold Hall')
    distance = route['distance']
    if not 1000 <= distance <= 20000:
        raise ValueError(f'Unexpected driving-route length: {distance:.0f} meters')
    return waypoints, distance


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path, help='Four-route simulation-routes.with-return.json')
    args = parser.parse_args()
    path = args.input
    if not path.is_file():
        parser.error(f'Input file not found: {path}')
    output = path.with_name('simulation-routes.with-both-returns.json')
    if output.exists():
        parser.error(f'Output already exists; refusing to overwrite: {output}')

    original = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(original, dict) or not isinstance(original.get('paths'), list):
        parser.error('Expected a JSON object with a paths array')
    paths = original['paths']
    pairs = [(r.get('fromStopId'), r.get('toStopId')) for r in paths]
    if len(pairs) != len(set(pairs)):
        parser.error('Input has duplicate directional route pairs')
    if (FROM_ID, TO_ID) in pairs:
        parser.error('Route 3 -> 2 already exists')
    missing = EXPECTED_PAIRS - set(pairs)
    if missing:
        parser.error(f'Input is missing existing routes: {sorted(missing)}. Use the four-route file.')

    waypoints, distance = fetch_route()
    updated = dict(original)
    updated['paths'] = paths + [{'fromStopId': FROM_ID, 'toStopId': TO_ID, 'waypoints': waypoints}]
    output.write_text(json.dumps(updated, indent=2) + '\n', encoding='utf-8')

    check = json.loads(output.read_text(encoding='utf-8'))
    assert check['paths'][:-1] == paths
    assert len(check['paths']) == len(paths) + 1
    print(f'Added Aviation Center (3) -> Nold Hall (2): {len(waypoints)} waypoints, {distance / 1000:.2f} km')
    print(f'Preserved original {len(paths)} routes without changes.')
    print(f'Input unchanged:  {path}')
    print(f'Review new file: {output}')
    print('Visually inspect the new driving route before integrating it.')


if __name__ == '__main__':
    try:
        main()
    except (HTTPError, URLError, TimeoutError, ValueError, KeyError, TypeError, json.JSONDecodeError) as exc:
        print(f'Route generation failed: {exc}', file=sys.stderr)
        sys.exit(1)
