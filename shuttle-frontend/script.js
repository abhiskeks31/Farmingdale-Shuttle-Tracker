// Get Started hides the splash screen and shows the map
function goToApp() {
    document.getElementById('splash').style.display = 'none';
    startMap();
}

const $ = id => document.getElementById(id);


// ---- talking to the backend ----

const API = 'http://localhost:8080/api';

// gets JSON from the backend, or null if it was not found (404)
async function getJson(path) {
    const response = await fetch(API + path);
    if (response.status === 404) return null;
    if (!response.ok) throw new Error('Request failed: ' + response.status);
    return response.json();
}

const ROUTE_COLORS = ['#7B40BA', '#4081BA', '#BA8140', '#2E9E6B', '#C0392B'];




// "farmingdale_lirr" -> "Farmingdale LIRR"
function prettyStopName(name) {
    return String(name || '').split('_').filter(Boolean).map(word =>
        word.toLowerCase() === 'lirr' ? 'LIRR' : word.charAt(0).toUpperCase() + word.slice(1)
    ).join(' ');
}

// stops text from being read as HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(ms) {
    return new Date(ms).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

// "07:55:00" -> "7:55 AM"
function formatClock(hhmmss) {
    const [hour, minute] = hhmmss.split(':').map(Number);
    const date = new Date();
    date.setHours(hour, minute, 0, 0);
    return date.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
}

// the time right now like "07:55", so it can be compared with the schedule
function nowAsClock() {
    const now = new Date();
    return String(now.getHours()).padStart(2, '0') + ':' + String(now.getMinutes()).padStart(2, '0');
}

const WEEK = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

// Does a service-day range such as "Mon-Fri" include today?
function runsToday(serviceDays) {
    const today = (new Date().getDay() + 6) % 7;   // Monday is 0
    return String(serviceDays).split(',').some(part => {
        const [start, end] = part.trim().split('-').map(day => WEEK.indexOf(day.trim().slice(0, 3)));
        if (start === -1) return false;
        if (end === undefined || end === -1) return today === start;
        return today >= start && today <= end;
    });
}


// ---- menu and panels ----

const toggle = $('nav-toggle');
const menu = $('nav-menu');
const panel = $('panel');
const menuItems = document.querySelectorAll('.nav-item');

function setMenu(open) {
    menu.classList.toggle('open', open);
    toggle.classList.toggle('is-open', open);
    toggle.setAttribute('aria-expanded', open);
    toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');

    // closing the menu closes the panel too
    if (!open) {
        panel.classList.remove('has-content');
        menuItems.forEach(item => item.removeAttribute('aria-pressed'));
    }
}

toggle.addEventListener('click', () => setMenu(!menu.classList.contains('open')));

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') setMenu(false);
});

// clicking a menu button highlights it and opens its panel
menuItems.forEach(item => {
    item.addEventListener('click', () => {
        menuItems.forEach(other => other.setAttribute('aria-pressed', other === item));
        panel.querySelectorAll('.view').forEach(view => {
            view.classList.toggle('active', view.id === item.dataset.view);
        });
        panel.classList.add('has-content');
    });
});


// ---- View Routes: one switch for each route ----

async function loadRouteToggles() {
    const box = $('route-toggles');

    let routes;
    try {
        routes = await getJson('/routes');
    } catch (err) {
        box.innerHTML = '<div class="empty">Could not load routes. Is the backend running?</div>';
        return;
    }
    if (!routes || routes.length === 0) {
        box.innerHTML = '<div class="empty">No routes available.</div>';
        return;
    }

    box.innerHTML = '';
    routes.forEach((route, i) => {
        const color = ROUTE_COLORS[i % ROUTE_COLORS.length];
        const name = escapeHtml(prettyStopName(route.routeName));

        const row = document.createElement('div');
        row.className = 'route-row';
        row.innerHTML = `
            <span class="toggle">
                <input type="checkbox" id="route-${i}" aria-label="Show ${name} route">
                <span class="track"></span><span class="knob"></span>
            </span>
            <label for="route-${i}"><span class="dot" style="background:${color}"></span>${name}</label>`;
        box.appendChild(row);

        // a switch draws or hides its route on the map (the code is in map.js)
        const checkbox = row.querySelector('input');
        checkbox.addEventListener('change', async () => {
            const drawn = await setRouteVisible(route, color, checkbox.checked);
            if (!drawn) checkbox.checked = false;
            $('route-msg').textContent = drawn ? '' : 'Could not draw this route.';
        });
    });
}

loadRouteToggles();


// ---- Search Routes ----

const fromSelect = $('search-from');
const toSelect = $('search-to');
const resultsBox = $('search-results');
const searchForm = $('search-form');
const detailBox = $('search-detail');

let currentResults = [];   // the trips on screen: { trip, stops, from, to }
let searchCount = 0;       // so an old, slow search can't replace a newer one
const tripStops = {};      // tripId -> its stops, so each trip is only asked for once

function fillStops(select, label, stops) {
    const options = stops.map(stop =>
        `<option value="${stop.stopId}">${escapeHtml(prettyStopName(stop.stopName))}</option>`).join('');
    select.innerHTML = `<option value="">${label}</option>${options}`;
}

async function loadStops() {
    try {
        const stops = await getJson('/stops');
        if (!stops) throw new Error('No stops');
        fillStops(fromSelect, 'From', stops);
        fillStops(toSelect, 'To', stops);
    } catch (err) {
        resultsBox.innerHTML = '<div class="empty">Could not load stops. Is the backend running?</div>';
    }
}

async function getTripStops(tripId) {
    if (!tripStops[tripId]) {
        tripStops[tripId] = (await getJson(`/trips/${tripId}/stops`)) || [];
    }
    return tripStops[tripId];
}

// Today's trips that go from one stop to another, earliest first.
// TODO: when the backend adds GET /trips/search?fromStopId=&toStopId=,
// replace this with one call to it.
async function findTrips(fromId, toId) {
    const trips = (await getJson('/trips')) || [];
    const todays = trips.filter(trip => runsToday(trip.serviceDays));

    const checked = await Promise.all(todays.map(async trip => {
        const stops = await getTripStops(trip.tripId);
        const from = stops.find(stop => String(stop.stopId) === String(fromId));
        const to = stops.find(stop => String(stop.stopId) === String(toId));
        return { trip, stops, from, to };
    }));

    const now = nowAsClock();
    return checked
        .filter(r => r.from && r.to && r.from.stopOrder < r.to.stopOrder)
        .filter(r => r.from.departureTime.slice(0, 5) >= now)   // not left yet
        .sort((a, b) => a.from.departureTime.localeCompare(b.from.departureTime));
}

async function showResults() {
    const thisSearch = ++searchCount;
    currentResults = [];

    if (!fromSelect.value || !toSelect.value) {
        resultsBox.innerHTML = '<div class="empty">Search results will appear here</div>';
        return;
    }
    if (fromSelect.value === toSelect.value) {
        resultsBox.innerHTML = '<div class="empty">Pick two different stops.</div>';
        return;
    }

    resultsBox.innerHTML = '<div class="empty">Searching...</div>';

    let results;
    try {
        results = await findTrips(fromSelect.value, toSelect.value);
    } catch (err) {
        if (thisSearch !== searchCount) return;
        resultsBox.innerHTML = '<div class="empty">Could not load trips. Please try again.</div>';
        return;
    }
    if (thisSearch !== searchCount) return;   // a newer search already started

    currentResults = results;

    if (results.length === 0) {
        resultsBox.innerHTML = '<div class="empty">No more trips today between these stops.</div>';
        return;
    }

    let html = '<p class="hint">Select More Info to see stop details.</p>';
    results.forEach((r, i) => {
        const destination = r.trip.finalDestination ? `To ${escapeHtml(r.trip.finalDestination)}` : 'Shuttle';
        html += `
            <div class="result">
                <span class="type">${destination}</span>
                <span class="time">${formatClock(r.from.departureTime)} &rarr; ${formatClock(r.to.departureTime)}</span>
                <button class="panel-link" type="button" data-trip="${i}">More Info &#9656;</button>
            </div>`;
    });
    resultsBox.innerHTML = html;
}

fromSelect.addEventListener('change', showResults);
toSelect.addEventListener('change', showResults);

// More Info shows the stops of that trip instead of the results
resultsBox.addEventListener('click', e => {
    const button = e.target.closest('button[data-trip]');
    if (!button) return;

    const result = currentResults[button.dataset.trip];
    // TODO: when live tracking is connected, show On Time / Delayed instead of "Scheduled"
    const rows = [...result.stops]
        .sort((a, b) => a.stopOrder - b.stopOrder)
        .map(stop => `
            <tr><td>${escapeHtml(prettyStopName(stop.stopName))}</td><td>${formatClock(stop.departureTime)}</td><td>Scheduled</td></tr>`)
        .join('');

    detailBox.innerHTML = `
        <div class="detail">
            <button class="panel-link" type="button" id="detail-back">&#9664; Back</button>
            <table>
                <thead><tr><th>Stops</th><th>Time</th><th>Status</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
        </div>`;

    $('detail-back').addEventListener('click', () => {
        detailBox.hidden = true;
        searchForm.hidden = false;
    });

    detailBox.hidden = false;
    searchForm.hidden = true;
    $('detail-back').focus();
});

loadStops();


// ---- Feedback ----

// fills the driver list from GET /api/drivers
async function loadDrivers() {
    const select = $('feedback-driver');
    try {
        const drivers = (await getJson('/drivers')) || [];
        select.innerHTML = '<option value="">Choose a driver</option>' + drivers.map(driver =>
            `<option value="${driver.driverId}">${escapeHtml(driver.driverName)}</option>`
        ).join('');
    } catch (err) {
        console.warn('Could not load the drivers.', err);
        select.innerHTML = '<option value="">No drivers available</option>';
        $('feedback-msg').textContent = 'Could not load the drivers. Is the backend running?';
    }
}

// sends the rating with POST /api/driver-feedback
$('feedback-form').addEventListener('submit', async e => {
    e.preventDefault();
    const driverId = Number($('feedback-driver').value);
    const checked = document.querySelector('input[name="rating"]:checked');
    if (!driverId || !checked) {
        $('feedback-msg').textContent = 'Pick a driver and a rating.';
        return;
    }

    const button = $('feedback-send');
    button.disabled = true;
    try {
        const response = await fetch(`${API}/driver-feedback`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ driverId, rating: Number(checked.value) })
        });
        if (!response.ok) throw new Error('Request failed: ' + response.status);
        $('feedback-msg').textContent = 'Thanks. Your rating was sent.';
        $('feedback-form').reset();
    } catch (err) {
        console.warn('Could not send the rating.', err);
        $('feedback-msg').textContent = 'Could not send your rating. Please try again.';
    } finally {
        button.disabled = false;
    }
});

loadDrivers();


// ---- Alerts and Admin Tools ----

// TODO: use the backend for alerts. For now they are saved in this browser only.
let alerts = [];
try {
    alerts = JSON.parse(localStorage.getItem('fst-alerts')) || [];
} catch (err) {
    // storage is blocked, so start with no alerts
}

function saveAlerts() {
    try {
        localStorage.setItem('fst-alerts', JSON.stringify(alerts));
    } catch (err) {
        // storage is blocked, the alerts just won't be remembered
    }
}

// fills the alert list in the Alert panel (riders) and in Admin Tools
function drawAlerts() {
    let adminHtml = '';
    let riderHtml = '';

    alerts.forEach(a => {
        const text = `<span class="text">${escapeHtml(a.text)}<span class="when">Posted ${formatTime(a.id)}</span></span>`;
        adminHtml += `<div class="alert-item">${text}<button class="remove" type="button" data-id="${a.id}" aria-label="Remove this alert">Remove</button></div>`;
        riderHtml = `<div class="alert-item">${text}</div>` + riderHtml;   // newest on top
    });

    $('admin-alerts').innerHTML = adminHtml || '<div class="empty">No active alerts.</div>';
    $('alert').innerHTML = '<h2>Alerts</h2>' + (riderHtml || '<div class="empty">No alerts right now.</div>');
    drawBell();
}


// ---- notification bell ----

// what this rider has cleared and read, saved in this browser
let bellState = { cleared: [], seenUpTo: 0 };
try {
    bellState = { ...bellState, ...JSON.parse(localStorage.getItem('fst-bell')) };
} catch (err) {
    // storage is blocked, so nothing is cleared or read yet
}

function saveBell() {
    try {
        localStorage.setItem('fst-bell', JSON.stringify(bellState));
    } catch (err) {
        // storage is blocked, clearing just won't be remembered
    }
}

const bell = $('bell');
const bellMenu = $('bell-menu');

function drawBell() {
    const shown = alerts.filter(a => !bellState.cleared.includes(a.id)).reverse();   // newest on top
    const unread = shown.filter(a => a.id > bellState.seenUpTo).length;

    $('bell-count').textContent = unread > 9 ? '9+' : unread;
    $('bell-count').hidden = unread === 0;
    bell.setAttribute('aria-label', unread ? `Notifications, ${unread} unread` : 'Notifications');

    $('bell-clear').hidden = shown.length === 0;
    $('bell-list').innerHTML = shown.map(a => `
        <div class="alert-item${a.id > bellState.seenUpTo ? ' unread' : ''}">
            <span class="text">${escapeHtml(a.text)}<span class="when">Posted ${formatTime(a.id)}</span></span>
            <button class="remove" type="button" data-id="${a.id}" aria-label="Clear this notification">&#10005;</button>
        </div>`).join('') || '<div class="empty">You\'re all caught up.</div>';
}

function setBell(open) {
    bellMenu.hidden = !open;
    bell.setAttribute('aria-expanded', open);
    if (open) return;

    // closing the list marks everything in it as read
    const newest = Math.max(0, ...alerts.map(a => a.id));
    if (newest > bellState.seenUpTo) {
        bellState.seenUpTo = newest;
        saveBell();
        drawBell();
    }
}

bell.addEventListener('click', () => setBell(bellMenu.hidden));

document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !bellMenu.hidden) setBell(false);
});

// clicking anywhere else closes the list
document.addEventListener('click', e => {
    // composedPath still knows where a removed button (like a cleared ✕) was
    const path = e.composedPath();
    if (!bellMenu.hidden && !path.includes(bellMenu) && !path.includes(bell)) setBell(false);
});

$('bell-list').addEventListener('click', e => {
    const button = e.target.closest('button[data-id]');
    if (!button) return;

    bellState.cleared.push(Number(button.dataset.id));
    saveBell();
    drawBell();
});

$('bell-clear').addEventListener('click', () => {
    alerts.forEach(a => {
        if (!bellState.cleared.includes(a.id)) bellState.cleared.push(a.id);
    });
    saveBell();
    drawBell();
});

// an alert posted in another tab shows up here right away
window.addEventListener('storage', e => {
    if (e.key !== 'fst-alerts') return;
    try {
        alerts = JSON.parse(e.newValue) || [];
    } catch (err) {
        alerts = [];
    }
    drawAlerts();
});

$('admin-login').addEventListener('submit', e => {
    e.preventDefault();
    // TODO: check the login with the backend. Right now any username and password works.
    $('admin-pass').value = '';
    $('admin-login').hidden = true;
    $('admin-tools').hidden = false;
    $('alert-text').focus();
});

$('alert-form').addEventListener('submit', e => {
    e.preventDefault();
    const text = $('alert-text').value.trim();
    if (!text) return;

    alerts.push({ id: Date.now(), text });
    saveAlerts();
    drawAlerts();
    $('alert-text').value = '';
    $('admin-msg').textContent = 'Alert posted.';
});

$('admin-alerts').addEventListener('click', e => {
    const button = e.target.closest('button[data-id]');
    if (!button) return;

    alerts = alerts.filter(a => String(a.id) !== button.dataset.id);
    bellState.cleared = bellState.cleared.filter(id => alerts.some(a => a.id === id));   // forget alerts that are gone
    saveAlerts();
    saveBell();
    drawAlerts();
    $('admin-msg').textContent = 'Alert removed.';
});

$('admin-signout').addEventListener('click', () => {
    $('admin-msg').textContent = '';
    $('admin-login').hidden = false;
    $('admin-tools').hidden = true;
    $('admin-user').focus();
});

drawAlerts();
