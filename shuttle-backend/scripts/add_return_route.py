"""Add the Nold Hall (2) -> Farmingdale LIRR (1) driving route to simulation-routes.json.
"""
import argparse
import json
import math
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

FROM_ID, TO_ID = 2, 1
NOLD = (40.749538, -73.4313365)
LIRR = (40.73588157993677, -73.44175266134374)


def meters(a, b):
    lat1, lon1 = map(math.radians, a)
    lat2, lon2 = map(math.radians, b)
    dlat, dlon = lat2 - lat1, lon2 - lon1
    h = math.sin(dlat / 2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin(dlon/2)**2
    return 6371000 * 2 * math.asin(min(1, math.sqrt(h)))


def load_route():
    # OSRM uses lon,lat, unlike the project's [lat,lon] waypoint format.
    coords = f'{NOLD[1]},{NOLD[0]};{LIRR[1]},{LIRR[0]}'
    query = urlencode({'overview': 'full', 'geometries': 'geojson', 'steps': 'false'})
    url = f'https://router.project-osrm.org/route/v1/driving/{coords}?{query}'
    request = Request(url, headers={'User-Agent': 'FarmingdaleShuttleTrackerCapstone/1.0'})
    with urlopen(request, timeout=30) as response:
        data = json.load(response)
    if data.get('code') != 'Ok' or not data.get('routes'):
        raise ValueError(f'Routing service returned: {data.get("code")} {data.get("message", "")}')
    route = data['routes'][0]
    geo = route['geometry']['coordinates']
    if len(geo) < 2:
        raise ValueError('Not enough route geometry points')
    waypoints = [[round(lat, 6), round(lon, 6)] for lon, lat in geo]
    if meters(waypoints[0], NOLD) > 150:
        raise ValueError('Route does not begin close to Nold Hall; review the stop coordinate')
    if meters(waypoints[-1], LIRR) > 150:
        raise ValueError('Route does not end close to Farmingdale LIRR; review the stop coordinate')
    if not 1000 <= route['distance'] <= 20000:
        raise ValueError('Unexpected route distance; review routing response')
    return waypoints, route['distance']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('input', type=Path, help='Path to original simulation-routes.json')
    args = parser.parse_args()
    path = args.input
    if not path.is_file():
        parser.error(f'File not found: {path}')
    doc = json.loads(path.read_text(encoding='utf-8'))
    if not isinstance(doc.get('paths'), list):
        parser.error('Expected top-level JSON object with a paths array')
    if any(r.get('fromStopId') == FROM_ID and r.get('toStopId') == TO_ID for r in doc['paths']):
        parser.error('Route 2 -> 1 already exists; no file created')
    existing = {(r.get('fromStopId'),r.get('toStopId')) for r in doc['paths']}
    if not {(1,2),(2,3),(1,3)}.issubset(existing):
        print('Warning: one or more expected forward routes not found in source', file=sys.stderr)
    waypoints, distance = load_route()
    doc['paths'].append({'fromStopId': FROM_ID, 'toStopId': TO_ID, 'waypoints': waypoints})
    out = path.with_name('simulation-routes.with-return.json')
    if out.exists():
        parser.error(f'Output exists, refusing to overwrite: {out}')
    out.write_text(json.dumps(doc, indent=2) + '\n', encoding='utf-8')
    check = json.loads(out.read_text(encoding='utf-8'))
    assert len(check['paths']) == len(existing) + 1, 'Unexpected duplicate route pairs in input'
    assert check['paths'][:-1] == json.loads(path.read_text(encoding='utf-8'))['paths'], 'Original routes modified'
    print(f'Added 2 -> 1 with {len(waypoints)} waypoints, routed distance {distance/1000:.2f} km')
    print(f'Original unchanged: {path}')
    print(f'Review new file:     {out}')
    print('Inspect the path on a map before using it for a public demonstration.')


if __name__ == '__main__':
    try:
        main()
    except (HTTPError, URLError, TimeoutError, ValueError, KeyError, json.JSONDecodeError) as exc:
        print(f'Failed to generate route: {exc}', file=sys.stderr)
        sys.exit(1)
