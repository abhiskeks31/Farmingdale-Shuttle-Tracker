// the map, made with Leaflet and OpenStreetMap
let map = null;
let stopLayer = null;       // all the stop markers
let shuttleLayer = null;    // all the shuttle markers
let noticeBox = null;       // small message box on the map

const REFRESH_MS = 5000;    // how often the shuttles move on the map
const shuttleMarkers = {};  // shuttleId -> marker
let stopsLoaded = false;


// each shuttle gets its own color so they are easy to tell apart
const SHUTTLE_COLORS = ['#006F71', '#7B40BA', '#D9622B', '#1F6FD1', '#C2185B'];

function shuttleColor(id) {
    const n = parseInt(id, 10);
    return SHUTTLE_COLORS[(Number.isFinite(n) ? n : 0) % SHUTTLE_COLORS.length];
}

function shuttleIcon(shuttle) {
    const color = shuttleColor(shuttle.shuttleId);
    return L.divIcon({
        className: 'shuttle-marker',
        html: `<div class="shuttle-pin" style="--pin: ${color}">
            <div class="shuttle-heading"></div>
            <svg viewBox="0 0 24 24" width="22" height="22" fill="white">
                <!-- bus body -->
                <rect x="2" y="6" width="20" height="11" rx="3"/>
                <!-- two windows and the windshield -->
                <rect x="4" y="8" width="5" height="4" fill="${color}"/>
                <rect x="10" y="8" width="5" height="4" fill="${color}"/>
                <rect x="16" y="8" width="4" height="4" rx="1" fill="${color}"/>
                <!-- passengers -->
                <circle cx="6.5" cy="10" r="1.2"/>
                <ellipse cx="6.5" cy="12.2" rx="1.8" ry="1"/>
                <circle cx="12.5" cy="10" r="1.2"/>
                <ellipse cx="12.5" cy="12.2" rx="1.8" ry="1"/>
                <!-- wheels -->
                <circle cx="7" cy="17.5" r="2.4"/>
                <circle cx="17" cy="17.5" r="2.4"/>
                <circle cx="7" cy="17.5" r="1" fill="${color}"/>
                <circle cx="17" cy="17.5" r="1" fill="${color}"/>
            </svg>
            <span class="shuttle-label">${escapeHtml(shuttle.vehicleNumber)}</span>
        </div>`,
        iconSize: [38, 38],
        iconAnchor: [19, 19],
        popupAnchor: [0, -20]
    });
}

const stopIcon = L.divIcon({
    className: 'stop-marker',
    html: '<div style="font-size:24px;line-height:24px;text-align:center;">&#128655;</div>',
    iconSize: [24, 24],
    iconAnchor: [12, 22],
    popupAnchor: [0, -20],
    tooltipAnchor: [12, -12]
});



// turns the arrow on a shuttle pin toward the way it is driving
function pointMarker(marker, from, lat, lng) {
    // ignore tiny GPS jitter (about 3 meters)
    if (Math.abs(from.lat - lat) < 0.00003 && Math.abs(from.lng - lng) < 0.00003) return;

    const toRad = d => d * Math.PI / 180;
    const y = Math.sin(toRad(lng - from.lng)) * Math.cos(toRad(lat));
    const x = Math.cos(toRad(from.lat)) * Math.sin(toRad(lat)) -
              Math.sin(toRad(from.lat)) * Math.cos(toRad(lat)) * Math.cos(toRad(lng - from.lng));
    const bearing = (Math.atan2(y, x) * 180 / Math.PI + 360) % 360;

    const arrow = marker.getElement() && marker.getElement().querySelector('.shuttle-heading');
    if (!arrow) return;
    arrow.style.transform = `rotate(${bearing}deg)`;
    arrow.style.opacity = '1';
}

// Converts database stop names into readable names for the map.
function formatStopName(stopName) {
    const names = {
        farmingdale_lirr: 'Farmingdale LIRR',
        nold_hall: 'Nold Hall',
        aviation_center: 'Aviation Center'
    };

    return names[stopName] ||
        stopName.replaceAll('_', ' ')
            .replace(/\b\w/g, char => char.toUpperCase());
}

// Formats the ETA response returned by Spring Boot.
// Handles running, stopped, and completed shuttle trips.
function formatShuttleETA(eta) {

    // No active simulation is available for this shuttle.
    if (!eta) {
        return 'ETA unavailable';
    }

    // Display an arrival estimate for a moving shuttle.
    if (eta.status === 'EN_ROUTE') {

        const seconds = Number(eta.etaSeconds);

        if (!Number.isFinite(seconds) || seconds < 0) {
            return 'ETA unavailable';
        }

        // Round up to avoid showing 0 minutes before arrival.
        const minutes = Math.max(1, Math.ceil(seconds / 60));

        // Format the database stop name before displaying it.
        const stop = eta.nextStopName
            ? escapeHtml(formatStopName(eta.nextStopName))
            : 'Unknown stop';

        return `Next stop: ${stop}<br>
                Estimated arrival: ~${minutes} min`;
    }

    if (eta.status === 'ARRIVED') {
        return 'Trip completed';
    }

    if (eta.status === 'NOT_RUNNING') {
        return 'Trip not running';
    }

    return 'ETA unavailable';
}

function shuttlePopup(shuttle, spot, eta) {
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
        ${formatShuttleETA(eta)}<br>
        Last update: ${time}`;
}

// shows a message in the corner of the map, or hides it when the text is empty
function showNotice(text) {
    if (!noticeBox) return;
    noticeBox.textContent = text;
    noticeBox.style.display = text ? 'block' : 'none';
}

//ive shuttles
// slides a marker from where it is now to the new spot instead of jumping
function glideMarker(marker, lat, lng, duration) {
    // stop an older slide that is still running for this marker
    if (marker._glide) cancelAnimationFrame(marker._glide);

    const start = marker.getLatLng();
    const startTime = performance.now();

    function step(now) {
        const t = Math.min((now - startTime) / duration, 1);
        marker.setLatLng([
            start.lat + (lat - start.lat) * t,
            start.lng + (lng - start.lng) * t
        ]);
        if (t < 1) marker._glide = requestAnimationFrame(step);
    }
    marker._glide = requestAnimationFrame(step);
}

async function refreshShuttles() {
    try {
        loadStopMarkers();   // does nothing once the stops are on the map
        const shuttles = await getJson('/shuttles');
        if (!shuttles) throw new Error('No shuttle list');

        // the newest GPS point of every shuttle, all asked at the same time
        const spots = await Promise.all(shuttles.map(shuttle =>
            getJson(`/gps-locations/shuttle/${shuttle.shuttleId}/latest`).catch(() => null)
        ));

        // Retrieve the latest ETA for each shuttle.
        // A missing or unavailable ETA must not prevent GPS markers
        // from appearing on the map.
        const etas = await Promise.all(shuttles.map(shuttle =>
            getJson(`/eta/shuttle/${shuttle.shuttleId}`)
                .catch(error => {
                    console.error(
                        `Could not load ETA for shuttle ${shuttle.shuttleId}:`,
                        error
                    );
                    return null;
                })
        ));

        const onMap = {};
        shuttles.forEach((shuttle, i) => {
            const spot = spots[i];
            const eta = etas[i];

            if (!spot) return;

            const lat = Number(spot.latitude);
            const lng = Number(spot.longitude);
            if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;

            onMap[shuttle.shuttleId] = true;
            const marker = shuttleMarkers[shuttle.shuttleId];

            if (marker) {
                // compare with where the shuttle is heading, not where it is mid-slide
                pointMarker(marker, marker._target || marker.getLatLng(), lat, lng);
                marker._target = L.latLng(lat, lng);
                glideMarker(marker, lat, lng, REFRESH_MS);
                marker.setPopupContent(shuttlePopup(shuttle, spot, eta));
            } else {
                shuttleMarkers[shuttle.shuttleId] = L.marker([lat, lng], {
                    icon: shuttleIcon(shuttle),
                    zIndexOffset: 1000,
                    title: shuttle.vehicleNumber
                }).addTo(shuttleLayer).bindPopup(shuttlePopup(shuttle, spot, eta));
            }
        });

        // take off the markers of shuttles that lost their position
        Object.keys(shuttleMarkers).forEach(id => {
            if (!onMap[id]) {
                cancelAnimationFrame(shuttleMarkers[id]._glide);
                shuttleLayer.removeLayer(shuttleMarkers[id]);
                delete shuttleMarkers[id];
            }
        });

        showNotice(Object.keys(onMap).length === 0 ? 'No shuttles are reporting a location right now.' : '');
    } catch (err) {
        showNotice('Cannot reach the shuttle server. Is the backend running?');
    }
}

// stop markers

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

// route lines (View Routes)

const routeLayers = {};   // routeId -> the drawn route, so it is only built once
const routeWanted = {};   // routeId -> true while its switch is on

// The stops of a route in order, as a list of stop lists.
// one trip for each different final destination.
// Retrieves the ordered stop sequences for a route.
// Uses Trip and Schedule data instead of the removed RouteStop table.
async function getRouteSequences(route) {

    // Retrieve all scheduled trips associated with this route.
    const trips = await getJson(
        `/trips/route/${route.routeId}`
    );

    if (!Array.isArray(trips) || trips.length === 0) {
        return [];
    }

    // Select one representative trip per final destination.
    const firstTrips = {};

    trips.forEach(trip => {
        const destination = trip.finalDestination || '';

        if (!firstTrips[destination]) {
            firstTrips[destination] = trip;
        }
    });

    // Retrieve scheduled stops in their correct travel order.
    const lists = await Promise.all(
        Object.values(firstTrips).map(trip =>
            getJson(`/trips/${trip.tripId}/stops`)
        )
    );

    // Return the ordered sequences to the existing drawing function.
    return lists
        .filter(list => Array.isArray(list) && list.length > 1)
        .map(list =>
            [...list].sort((a, b) => a.stopOrder - b.stopOrder)
        );
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
            // white outline underneath so the colored line stands out from the map
            L.polyline(points, { color: 'white', weight: 9, opacity: 0.9, lineCap: 'round', lineJoin: 'round' }).addTo(group);
            L.polyline(points, { color, weight: 5, opacity: 1, lineCap: 'round', lineJoin: 'round' }).addTo(group);
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
