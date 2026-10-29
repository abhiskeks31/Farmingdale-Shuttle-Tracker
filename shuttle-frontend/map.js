// the map, made with Leaflet and OpenStreetMap
let map = null;
let stopLayer = null;       // all the stop markers
let shuttleLayer = null;    // all the shuttle markers
let noticeBox = null;       // small message box on the map

const REFRESH_MS = 5000;    // how often the shuttles move on the map
const shuttleMarkers = {};  // shuttleId -> marker
let stopsLoaded = false;


// ---- icons and popups ----

const shuttleIcon = L.divIcon({
    className: 'shuttle-marker',
    html: '<div style="font-size:28px;line-height:28px;text-align:center;">&#128652;</div>',
    iconSize: [28, 28],
    iconAnchor: [14, 14],
    popupAnchor: [0, -14]
});

const stopIcon = L.divIcon({
    className: 'stop-marker',
    html: '<div style="font-size:24px;line-height:24px;text-align:center;">&#128655;</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 22],
    popupAnchor: [0, -20],
    tooltipAnchor: [12, -12]
});



function shuttlePopup(shuttle, spot) {
    const capacity = Number(shuttle.capacity) || 0;
    const passengers = Number(shuttle.currentPassengers) || 0;
    const seats = Math.max(capacity - passengers, 0);
    const percent = capacity > 0 ? Math.min(100, Math.round(passengers / capacity * 100)) : 0;
    const time = spot.recordedAt
        ? new Date(spot.recordedAt).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit', second: '2-digit' })
        : 'unknown';

    return `
        <strong>${escapeHtml(shuttle.vehicleNumber)}</strong><br>
        Status: ${escapeHtml(shuttle.status)}<br>
        ${passengers} of ${capacity} aboard (${seats} seats open)
        <div style="background:#e5e5e5;border-radius:4px;height:6px;margin:4px 0 6px;">
            <div style="background:#006F71;width:${percent}%;height:6px;border-radius:4px;"></div>
        </div>
        Last update: ${time}`;
}

// shows a message in the corner of the map, or hides it when the text is empty
function showNotice(text) {
    if (!noticeBox) return;
    noticeBox.textContent = text;
    noticeBox.style.display = text ? 'block' : 'none';
}


// ---- keep the shuttles driving ----

// the backend only moves a shuttle after it is told to start a trip,
// and stops it at the end of the trip, so the map starts the next one
const nextTrip = {};
const starting = {};

async function keepDriving(shuttle, i, all) {
    const id = shuttle.shuttleId;
    if (starting[id]) return;
    starting[id] = true;

    try {
        const status = await getJson(`/simulation/shuttle/${id}/status`);
        if (status && status.running) return;   // already driving

        const trips = await getJson(`/trips/route/${shuttle.routeId}`);
        if (!trips || trips.length === 0) return;

        // the shuttles begin spread out over the trip list, so they don't all drive the same trip
        if (nextTrip[id] === undefined) nextTrip[id] = Math.floor(i * trips.length / all.length);

        // try each trip once, some have only one stop and cannot start
        for (let tries = 0; tries < trips.length; tries++) {
            const place = nextTrip[id] % trips.length;
            nextTrip[id] = place + 1;

            const response = await fetch(
                `${API}/simulation/shuttle/${id}/start?tripId=${trips[place].tripId}`,
                { method: 'POST' }
            );
            if (response.ok || response.status === 409) return;   // 409: already started
        }
    } catch (err) {
        // the next refresh tries again
    } finally {
        starting[id] = false;
    }
}

// ---- live shuttles ----

async function refreshShuttles() {
    try {
        loadStopMarkers();   // does nothing once the stops are on the map
        const shuttles = await getJson('/shuttles');
        if (!shuttles) throw new Error('No shuttle list');

        shuttles.forEach(keepDriving);

        // the newest GPS point of every shuttle, all asked at the same time
        const spots = await Promise.all(shuttles.map(shuttle =>
            getJson(`/gps-locations/shuttle/${shuttle.shuttleId}/latest`).catch(() => null)
        ));

        const onMap = {};
        shuttles.forEach((shuttle, i) => {
            const spot = spots[i];
            if (!spot) return;   // this shuttle has no position yet

            const lat = Number(spot.latitude);
            const lng = Number(spot.longitude);
            if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

            onMap[shuttle.shuttleId] = true;
            const marker = shuttleMarkers[shuttle.shuttleId];

            if (marker) {
                marker.setLatLng([lat, lng]);
                marker.setPopupContent(shuttlePopup(shuttle, spot));
            } else {
                shuttleMarkers[shuttle.shuttleId] = L.marker([lat, lng], {
                    icon: shuttleIcon,
                    zIndexOffset: 1000,
                    title: shuttle.vehicleNumber
                }).addTo(shuttleLayer).bindPopup(shuttlePopup(shuttle, spot));
            }
        });

        // take off the markers of shuttles that lost their position
        Object.keys(shuttleMarkers).forEach(id => {
            if (!onMap[id]) {
                shuttleLayer.removeLayer(shuttleMarkers[id]);
                delete shuttleMarkers[id];
            }
        });

        showNotice(Object.keys(onMap).length === 0 ? 'No shuttles are reporting a location right now.' : '');
    } catch (err) {
        showNotice('Cannot reach the shuttle server. Is the backend running?');
    }
}

// ---- stop markers ----

async function loadStopMarkers() {
    if (stopsLoaded) return;
    stopsLoaded = true;   // so two refreshes don't load the stops twice

    try {
        const stops = await getJson('/stops');
        if (!stops) throw new Error('No stops');

        // stops at the exact same spot share one marker, so none is hidden
        const places = {};
        stops.forEach(stop => {
            const lat = Number(stop.latitude);
            const lng = Number(stop.longitude);
            if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

            const key = lat.toFixed(5) + ',' + lng.toFixed(5);
            if (!places[key]) places[key] = { lat, lng, names: [] };
            places[key].names.push(prettyStopName(stop.stopName));
        });

        Object.values(places).forEach(place => {
            const name = place.names.join(' / ');
            L.marker([place.lat, place.lng], { icon: stopIcon, title: name })
                .bindTooltip(escapeHtml(name))
                .bindPopup('<strong>' + escapeHtml(name) + '</strong><br>Shuttle stop')
                .addTo(stopLayer);
        });
    } catch (err) {
        stopsLoaded = false;   // try again on the next refresh
    }
}

// ---- route lines (View Routes) ----

const routeLayers = {};   // routeId -> the drawn route, so it is only built once
const routeWanted = {};   // routeId -> true while its switch is on

// The stops of a route in order, as a list of stop lists.
// one trip for each different final destination.
async function getRouteSequences(route) {
    const routeStops = await getJson(`/route-stops/route/${route.routeId}`);
    if (routeStops && routeStops.length > 1) {
        const stops = (await getJson('/stops')) || [];
        const sequence = [...routeStops]
            .sort((a, b) => a.stopOrder - b.stopOrder)
            .map(rs => stops.find(stop => stop.stopId === rs.stopId))
            .filter(Boolean);
        if (sequence.length > 1) return [sequence];
    }

    const trips = await getJson(`/trips/route/${route.routeId}`);
    if (!trips || trips.length === 0) return [];

    const firstTrips = {};   // final destination -> the first trip that goes there
    trips.forEach(trip => {
        const place = trip.finalDestination || '';
        if (!firstTrips[place]) firstTrips[place] = trip;
    });

    const lists = await Promise.all(
        Object.values(firstTrips).map(trip => getJson(`/trips/${trip.tripId}/stops`))
    );
    return lists
        .filter(list => list && list.length > 1)
        .map(list => [...list].sort((a, b) => a.stopOrder - b.stopOrder));
}

// draws the line of one route
async function buildRouteLayer(route, color) {
    const sequences = await getRouteSequences(route);
    if (sequences.length === 0) throw new Error('This route has no stops yet');

    const group = L.layerGroup();
    const allPoints = [];
    const drawnPairs = {};

    for (const sequence of sequences) {
        // one piece of line between each two stops that follow each other
        for (let i = 0; i < sequence.length - 1; i++) {
            const a = sequence[i];
            const b = sequence[i + 1];
            const key = `${a.stopId}-${b.stopId}`;
            if (drawnPairs[key]) continue;
            drawnPairs[key] = true;

            // the backend has road points for some pairs, if not use a straight line
            let points = null;
            try {
                const road = await getJson(`/simulation/routes/${a.stopId}/${b.stopId}`);
                if (road && Array.isArray(road.waypoints) && road.waypoints.length > 1) points = road.waypoints;
            } catch (err) {
                // no road points, the straight line is used below
            }
            if (!points) {
                points = [
                    [Number(a.latitude), Number(a.longitude)],
                    [Number(b.latitude), Number(b.longitude)]
                ];
            }

            allPoints.push(...points);
            L.polyline(points, { color, weight: 5, opacity: 0.85 }).addTo(group);
        }

        // make sure every stop is inside the zoom, even if a piece of line is missing
        sequence.forEach(stop => allPoints.push([Number(stop.latitude), Number(stop.longitude)]));
    }

    group.routeBounds = allPoints.length ? L.latLngBounds(allPoints) : null;
    return group;
}

// the View Routes switch calls this. Gives false if the route could not be drawn.
async function setRouteVisible(route, color, on) {
    if (!map) return false;
    routeWanted[route.routeId] = on;

    if (!on) {
        if (routeLayers[route.routeId]) map.removeLayer(routeLayers[route.routeId]);
        return true;
    }

    try {
        if (!routeLayers[route.routeId]) {
            routeLayers[route.routeId] = await buildRouteLayer(route, color);
        }
        // the switch may have been turned off while the route was loading
        if (!routeWanted[route.routeId]) return true;

        const layer = routeLayers[route.routeId];
        layer.addTo(map);
        if (layer.routeBounds) map.fitBounds(layer.routeBounds, { padding: [40, 40] });
        return true;
    } catch (err) {
        return false;
    }
}


// Get Started calls this
function startMap() {
    document.getElementById('map').style.display = 'block';

    // the map was already made, so just fix its size
    if (map) {
        map.invalidateSize();
        return;
    }

    // centered on Farmingdale State College
    map = L.map('map').setView([40.7497, -73.4285], 15);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(map);

    stopLayer = L.layerGroup().addTo(map);
    shuttleLayer = L.layerGroup().addTo(map);

    // message box in the bottom left corner of the map
    const Notice = L.Control.extend({
        options: { position: 'bottomleft' },
        onAdd() {
            noticeBox = L.DomUtil.create('div');
            noticeBox.style.cssText = 'display:none;background:#fff;padding:8px 12px;border-radius:8px;' +
                'box-shadow:0 1px 6px rgba(0,0,0,.3);font:14px Poppins,sans-serif;max-width:260px;';
            return noticeBox;
        }
    });
    map.addControl(new Notice());

    refreshShuttles();
    setInterval(refreshShuttles, REFRESH_MS);
}
