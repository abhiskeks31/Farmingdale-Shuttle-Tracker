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

const ROUTE_COLORS = ['#2D9CDB', '#E8505B', '#9B51E0', '#F2994A', '#27AE60'];


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

// Does a service-day range such as "Mon-Fri" include today
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


// the Back button closes the open panel but keeps the menu open
document.getElementById('panel-back').addEventListener('click', () => {
    panel.classList.remove('has-content');
    menuItems.forEach(item => item.removeAttribute('aria-pressed'));
    menu.querySelector('.nav-item').focus();
});


// ---- view routes one switch for each route ----

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

// -- Search Routes --

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
// need to replace this with one call to it.
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
                <span class="time"><strong>${formatClock(r.from.departureTime)}</strong> &rarr; ${formatClock(r.to.departureTime)}</span>
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

// ---- Student Feedback ----
// Students submit an 8-character verification code and a rating.
// Spring Boot identifies the correct driver from the trip session.
$('feedback-form').addEventListener('submit', async e => {

    e.preventDefault();

    const message = $('feedback-msg');
    const button = $('feedback-send');

    const code = $('feedback-code')
        .value.trim().toUpperCase();

    const selectedRating = document.querySelector(
        'input[name="rating"]:checked'
    );

    // Validate the input before sending it to Spring Boot.
    if (!/^[A-HJ-NP-Z2-9]{8}$/.test(code) || !selectedRating) {
        message.textContent =
            'Enter a valid 8-character trip code and select a rating.';
        return;
    }

    button.disabled = true;
    message.textContent = 'Submitting feedback...';

    try {

        // Retrieve a CSRF token from Spring Security.
        const csrfResponse = await fetch(
            `${API}/auth/csrf`,
            { credentials: 'include' }
        );

        if (!csrfResponse.ok) {
            throw new Error('Could not retrieve CSRF token');
        }

        const csrf = await csrfResponse.json();

        // Submit the code and rating to our feedback endpoint.
        const response = await fetch(`${API}/feedback`, {

            method: 'POST',

            credentials: 'include',

            headers: {
                'Content-Type': 'application/json',
                [csrf.headerName || 'X-CSRF-TOKEN']: csrf.token
            },

            body: JSON.stringify({
                verificationCode: code,
                rating: Number(selectedRating.value)
            })
        });

        // Display the correct message for each response.
        if (response.status === 400) {

            message.textContent =
                'Invalid verification code or rating. Please check your entry.';

        } else if (response.status === 410) {

            message.textContent =
                'This verification code has expired.';

        } else if (response.status === 403) {

            message.textContent =
                'Security verification failed. Refresh and try again.';

        } else if (response.status === 429) {

            message.textContent =
                'Too many attempts. Please try again later.';

        } else if (!response.ok) {

            throw new Error(
                'Feedback request failed: ' + response.status
            );

        } else {

            // Successful submission.
            $('feedback-form').reset();

            message.textContent =
                'Thank you! Your driver rating was submitted successfully.';
        }

    } catch (err) {

        console.error('Feedback submission failed:', err);

        message.textContent =
            'Could not submit feedback. Please check the backend connection.';

    } finally {

        button.disabled = false;
    }
});

// ---- Alerts and Admin Tools ----
let alerts = [];

// Retrieves active alerts from the Spring Boot backend.
// Alerts are stored in Supabase rather than the browser's localStorage.
async function loadAlerts() {
    try {
        // Send a public GET request to retrieve active alerts.
        const response = await fetch(`${API}/alerts`);

        // Throw an error if the backend request fails.
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        // Convert the JSON response into our alerts array.
        alerts = await response.json();

        // Refresh the alerts displayed on the frontend.
        drawAlerts();
        drawBell();

    } catch (error) {
        // Log errors to help with debugging.
        console.error("Failed to load alerts:", error);

        // Inform the user if alerts cannot be retrieved.
        // Previously loaded alerts remain in the alerts array.
        $('alert').innerHTML =
            '<h2>Alerts</h2><div class="empty">Unable to load alerts.</div>';
    }

}

// Sends a new alert to Spring Boot using the logged-in admin session.
async function postAlert(title, message, alertType = 'INFO') {

    // Retrieve the current CSRF token for this session.
    const csrfResponse = await fetch(`${API}/auth/csrf`, {
        credentials: 'include'
    });

    if (!csrfResponse.ok) {
        throw new Error('Unable to retrieve CSRF token.');
    }

    const csrf = await csrfResponse.json();

    // Send the announcement to the protected alert endpoint.
    const response = await fetch(`${API}/alerts`, {
        method: 'POST',
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            [csrf.headerName]: csrf.token
        },
        body: JSON.stringify({
            title: title.trim(),
            message: message.trim(),
            alertType: alertType,
            expiresAt: null
        })
    });

    // Handle common authentication and validation failures.
    if (response.status === 401) {
        throw new Error('Please log in as an administrator.');
    }

    if (response.status === 403) {
        throw new Error('Access denied or invalid CSRF token.');
    }

    if (!response.ok) {
        throw new Error(`Failed to post alert (HTTP ${response.status}).`);
    }

    // Reload alerts so the student page and bell update.
    await loadAlerts();

    return true;
}

// Deactivates an existing alert through the Spring Boot backend.
// Only authenticated administrators can perform this operation.
async function deactivateAlert(alertId) {

    // Retrieve a fresh CSRF token for the current admin session.
    const csrfResponse = await fetch(`${API}/auth/csrf`, {
        credentials: 'include'
    });

    if (!csrfResponse.ok) {
        throw new Error('Unable to retrieve CSRF token.');
    }

    const csrf = await csrfResponse.json();

    // Send a protected PATCH request to deactivate the alert.
    const response = await fetch(
        `${API}/alerts/${encodeURIComponent(alertId)}/deactivate`,
        {
            method: 'PATCH',
            credentials: 'include',
            headers: {
                [csrf.headerName]: csrf.token
            }
        }
    );

    if (response.status === 401) {
        throw new Error('Please log in as an administrator.');
    }

    if (response.status === 403) {
        throw new Error('Access denied or invalid CSRF token.');
    }

    if (response.status === 404) {
        throw new Error('Alert not found.');
    }

    if (!response.ok) {
        throw new Error(
            `Failed to deactivate alert (HTTP ${response.status}).`
        );
    }

    // Reload active alerts so removed announcements disappear
    // from the student page, admin list, and notification bell.
    await loadAlerts();
}

// Displays backend alerts in both the admin and student interfaces.
function drawAlerts() {

    // Store the generated HTML for each interface separately.
    let adminHtml = '';
    let riderHtml = '';

    // Loop through every active alert returned by the backend.
    alerts.forEach(alert => {

        // Escape user-provided text to prevent HTML injection.
        const title = escapeHtml(alert.title);
        const message = escapeHtml(alert.message);
        const type = escapeHtml(alert.alertType);

        // Format the alert's creation timestamp for display.
        const posted = alert.createdAt
            ? formatTime(new Date(alert.createdAt).getTime())
            : 'Recently';

        // Create the shared alert content.
        const content = `
            <span class="text">
                <strong>${title}</strong>
                <span class="hint">${type}</span>
                <span>${message}</span>
                <span class="when">Posted ${posted}</span>
            </span>
        `;

        // Admins receive an additional Remove button.
        // The button will later call the backend deactivation endpoint.
        adminHtml += `
            <div class="alert-item">
                ${content}
                <button class="remove"
                        type="button"
                        data-id="${alert.alertId}">
                    Remove
                </button>
            </div>
        `;

        // Students can view announcements but cannot remove them.
        riderHtml += `
            <div class="alert-item">
                ${content}
            </div>
        `;
    });

    // Update the administrative alerts list.
    $('admin-alerts').innerHTML =
        adminHtml || '<div class="empty">No active alerts.</div>';

    // Update the student-facing alerts page.
    $('alert').innerHTML =
        '<h2>Alerts</h2>' +
        (riderHtml || '<div class="empty">No alerts right now.</div>');
}

// Load alerts when the webpage initially opens.
loadAlerts();

// Check Supabase for updated announcements every 30 seconds.
// This allows new or deactivated alerts to appear without a page refresh.
setInterval(loadAlerts, 30000);

// ---- notification bell ----

// what this rider has cleared and read, saved in this browser
let bellState = { cleared: [], seenUpTo: 0 };
try {
    bellState = { ...bellState, ...JSON.parse(localStorage.getItem('fst-bell')) };
} catch (err) {

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

// Displays active backend alerts in the notification bell.
    function drawBell() {

        // Exclude notifications dismissed by this browser.
        // Sort newest first without changing the original alerts array.
        const shown = alerts
            .filter(a => !bellState.cleared.includes(a.alertId))
            .sort((a, b) => b.alertId - a.alertId);

        // Count alerts posted after the last notification viewed.
        const unread = shown.filter(
            a => a.alertId > bellState.seenUpTo
        ).length;

        // Update the notification badge.
        $('bell-count').textContent = unread > 9 ? '9+' : unread;
        $('bell-count').hidden = unread === 0;

        bell.setAttribute(
            'aria-label',
            unread
                ? `Notifications, ${unread} unread`
                : 'Notifications'
        );

        // Hide the Clear All button when there are no notifications.
        $('bell-clear').hidden = shown.length === 0;

        // Generate the notification list using backend alert fields.
        $('bell-list').innerHTML = shown.map(a => {

                // Escape database text before inserting it into HTML.
                const title = escapeHtml(a.title);
                const message = escapeHtml(a.message);
                const type = escapeHtml(a.alertType);

                // Convert the backend timestamp into a readable date.
                const posted = a.createdAt
                    ? formatTime(new Date(a.createdAt).getTime())
                    : 'Recently';

                const unreadClass =
                    a.alertId > bellState.seenUpTo ? ' unread' : '';

                return `
            <div class="alert-item${unreadClass}">
                <span class="text">
                    <strong>${title}</strong>
                    <span class="hint">${type}</span>
                    <span>${message}</span>
                    <span class="when">Posted ${posted}</span>
                </span>

                <button class="remove"
                        type="button"
                        data-id="${a.alertId}"
                        aria-label="Clear this notification">
                    &#10005;
                </button>
            </div>
        `;
            }).join('') ||
            '<div class="empty">You\'re all caught up.</div>';
    }


// Opens or closes the notification menu.
// Closing the menu marks all currently displayed alerts as read.
function setBell(open) {

    bellMenu.hidden = !open;
    bell.setAttribute('aria-expanded', open);

    if (open) return;

    // Get the newest alert ID currently available.
    const newest = Math.max(
        0,
        ...alerts.map(a => a.alertId)
    );

    // Remember which alerts have been viewed.
    if (newest > bellState.seenUpTo) {
        bellState.seenUpTo = newest;
        saveBell();
    }

    // Refresh the notification count after closing.
    drawBell();
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


// Clears a single notification for this browser.
// This does NOT delete or deactivate the alert in Supabase.
$('bell-list').addEventListener('click', e => {

    const button = e.target.closest('button[data-id]');
    if (!button) return;

    const alertId = Number(button.dataset.id);

    // Avoid storing duplicate dismissed alert IDs.
    if (!bellState.cleared.includes(alertId)) {
        bellState.cleared.push(alertId);
    }

    saveBell();
    drawBell();
});

// Clears all currently displayed notifications.
// Backend alerts remain active and available on the Alerts page.
$('bell-clear').addEventListener('click', () => {

    alerts.forEach(a => {
        if (!bellState.cleared.includes(a.alertId)) {
            bellState.cleared.push(a.alertId);
        }
    });

    saveBell();
    drawBell();
});

/*
 * Admin Login
 * Authenticates administrators using Spring Security.
 * Credentials are verified against the Supabase admin table.
 */
$('admin-login').addEventListener('submit', async e => {
    e.preventDefault();

    const username = $('admin-user').value;
    const password = $('admin-pass').value;
    const loginMsg = $('login-msg');

    loginMsg.textContent = 'Signing in...';

    try {
        // Step 1: Retrieve a CSRF token and session cookie.
        const csrfResponse = await fetch(`${API}/auth/csrf`, {
            credentials: 'include'
        });

        if (!csrfResponse.ok) {
            throw new Error('Could not retrieve CSRF token');
        }

        const csrf = await csrfResponse.json();

        // Step 2: Submit login credentials to Spring Security.
        const loginResponse = await fetch(`${API}/auth/login`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                [csrf.headerName]: csrf.token
            },
            body: new URLSearchParams({
                username: username,
                password: password
            })
        });

        if (!loginResponse.ok) {
            loginMsg.textContent = 'Invalid username or password.';
            return;
        }

        // Step 3: Verify the authenticated session.
        const adminResponse = await fetch(`${API}/auth/me`, {
            credentials: 'include'
        });

        if (!adminResponse.ok) {
            throw new Error('Could not verify admin session');
        }

        const admin = await adminResponse.json();

        // Step 4: Display the existing admin tools.
        $('admin-pass').value = '';
        $('admin-login').hidden = true;
        $('admin-tools').hidden = false;

        $('admin-msg').textContent =
            `Logged in as ${admin.username}`;

    } catch (error) {
        console.error('Admin login failed:', error);
        loginMsg.textContent =
            'Unable to sign in. Please try again.';
    }
});

// Posts a new announcement using the authenticated admin session.
// Alerts are saved in Supabase through the Spring Boot REST API.
$('alert-form').addEventListener('submit', async e => {
    e.preventDefault();

    const message = $('alert-text').value.trim();
    const adminMsg = $('admin-msg');
    const submitButton = e.currentTarget.querySelector('[type="submit"]');

    // Prevent empty announcements.
    if (!message) {
        adminMsg.textContent = 'Please enter an alert message.';
        return;
    }

    // Temporarily disable submission to prevent duplicate alerts.
    if (submitButton) submitButton.disabled = true;
    adminMsg.textContent = 'Posting alert...';

    try {
        // The current form has only a message field.
        // Use a default title and alert type until we add more inputs.
        await postAlert(
            'Shuttle Service Alert',
            message,
            'INFO'
        );

        // Clear the form only after the backend accepts the alert.
        $('alert-text').value = '';
        adminMsg.textContent = 'Alert posted successfully!';

    } catch (error) {
        console.error('Alert posting failed:', error);
        adminMsg.textContent = error.message;

    } finally {
        if (submitButton) submitButton.disabled = false;
    }
});

// Handles administrator removal of active service alerts.
// Deactivation preserves the database record for future reference.
$('admin-alerts').addEventListener('click', async e => {

    const button = e.target.closest('button[data-id]');
    if (!button) return;

    const alertId = button.dataset.id;
    const adminMsg = $('admin-msg');

    // Disable the button while the request is being processed.
    button.disabled = true;
    adminMsg.textContent = 'Removing alert...';

    try {
        // Deactivate the alert in Supabase through Spring Boot.
        await deactivateAlert(alertId);

        // The backend GET request refreshes all alert displays.
        adminMsg.textContent = 'Alert removed successfully!';

    } catch (error) {
        console.error('Alert deactivation failed:', error);

        // Keep the alert visible if the request failed.
        adminMsg.textContent = error.message;

        button.disabled = false;
    }
});

/*
 * Admin Logout
 * Ends the authenticated Spring Security session.
 */
$('admin-signout').addEventListener('click', async () => {

    const adminMsg = $('admin-msg');
    const signoutButton = $('admin-signout');

    adminMsg.textContent = 'Signing out...';
    signoutButton.disabled = true;

    try {
        // Step 1: Retrieve a fresh CSRF token.
        const csrfResponse = await fetch(`${API}/auth/csrf`, {
            credentials: 'include'
        });

        if (!csrfResponse.ok) {
            throw new Error('Could not retrieve CSRF token');
        }

        const csrf = await csrfResponse.json();

        // Step 2: Send logout request to Spring Security.
        const logoutResponse = await fetch(`${API}/auth/logout`, {
            method: 'POST',
            credentials: 'include',
            headers: {
                [csrf.headerName]: csrf.token
            }
        });

        if (!logoutResponse.ok) {
            throw new Error('Logout failed');
        }

        // Step 3: Reset the frontend after successful logout.
        $('admin-login').reset();
        $('admin-login').hidden = false;
        $('admin-tools').hidden = true;

        // Remove verification codes from the interface after logout.
        $('feedback-codes-list').replaceChildren();
        $('feedback-codes-msg').textContent = '';

        $('login-msg').textContent = 'Signed out successfully.';
        adminMsg.textContent = '';

        $('admin-user').focus();

    } catch (error) {
        console.error('Admin logout failed:', error);
        adminMsg.textContent = 'Unable to sign out. Please try again.';

    } finally {
        signoutButton.disabled = false;
    }

});

/*
   * Restore Admin Session
   * Checks whether the browser already has an authenticated
   * Spring Security session when the website loads.
   */
async function restoreAdminSession() {
    try {
        const response = await fetch(`${API}/auth/me`, {
            credentials: 'include'
        });

        // No authenticated session exists.
        if (response.status === 401 || response.status === 403) {
            $('admin-login').hidden = false;
            $('admin-tools').hidden = true;
            return;
        }

        if (!response.ok) {
            throw new Error('Could not check admin session');
        }

        // An authenticated administrator session exists.
        const admin = await response.json();

        $('admin-login').hidden = true;
        $('admin-tools').hidden = false;

        $('admin-msg').textContent =
            `Logged in as ${admin.username}`;

    } catch (error) {
        console.error('Session restoration failed:', error);

        // Keep admin controls hidden if verification fails.
        $('admin-login').hidden = false;
        $('admin-tools').hidden = true;

        $('login-msg').textContent =
            'Could not verify admin session.';
    }
}

// ---- Admin Tools: seats and trips ----

// sends a POST that needs the admin login (gets a CSRF token first)
async function adminPost(path, body) {
    const csrfRes = await fetch(`${API}/auth/csrf`, {
        credentials: 'include',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
    });
    if (!csrfRes.ok) throw new Error('csrf');
    const csrf = await csrfRes.json();

    const headers = {
        'X-Requested-With': 'XMLHttpRequest',
        [csrf.headerName || 'X-CSRF-TOKEN']: csrf.token
    };
    if (body) headers['Content-Type'] = 'application/json';

    return fetch(API + path, {
        method: 'POST',
        credentials: 'include',
        headers,
        body: body ? JSON.stringify(body) : undefined
    });
}

/// a short message for a request that did not work
function whyFailed(response, what) {
    if (response.status === 401 || response.status === 403) return 'Please sign in again.';
    if (response.status === 409) return 'That bus is already running.';
    if (response.status === 404 && what === 'stop') return 'That bus is not running a trip right now.';
    if (response.status === 400) {
        return what === 'seats'
            ? 'Those numbers do not fit this bus. Check boarding, exiting and capacity.'
            : 'This could not be done. A trip needs 2 or more stops.';
    }
    return 'Something went wrong (' + response.status + ').';
}

// fills the shuttle and trip lists
async function loadAdminLists() {
    try {
        const shuttles = (await getJson('/shuttles')) || [];
        const shuttleOptions = shuttles.map(s =>
            `<option value="${s.shuttleId}">Bus ${escapeHtml(s.vehicleNumber)}</option>`).join('');
        $('occ-shuttle').innerHTML = shuttleOptions;
        $('trip-shuttle').innerHTML = shuttleOptions;
        $('feedback-shuttle').innerHTML = shuttleOptions;

        const trips = (await getJson('/trips')) || [];
        $('trip-trip').innerHTML = trips.map(t =>
            `<option value="${t.tripId}">Trip ${t.tripId} to ${escapeHtml(t.finalDestination || 'shuttle')} (${escapeHtml(t.serviceDays)})</option>`).join('');
    } catch (err) {
        $('occ-msg').textContent = 'Could not load buses and trips. Is the backend running?';
    }
}


// ---- Driver Ratings: Load Driver List ----
// Populates the dropdown using the existing public drivers API.
async function loadRatingsDrivers() {

    const select = $('ratings-driver');

    try {
        const drivers = (await getJson('/drivers')) || [];

        select.replaceChildren();

        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = 'Choose a driver';
        select.appendChild(placeholder);

        drivers.forEach(driver => {

            const option = document.createElement('option');

            option.value = driver.driverId;
            option.textContent = driver.driverName;

            select.appendChild(option);
        });

    } catch (error) {

        console.error('Could not load drivers:', error);

        select.innerHTML =
            '<option value="">Unable to load drivers</option>';

        $('ratings-msg').textContent =
            'Could not load drivers. Check the backend connection.';
    }
}

// Update seats
$('occupancy-form').addEventListener('submit', async e => {
    e.preventDefault();
    const msg = $('occ-msg');
    msg.textContent = 'Updating...';
    try {
        const response = await adminPost(`/occupancy/shuttle/${$('occ-shuttle').value}`, {
            boarding: Number($('occ-boarding').value),
            exiting: Number($('occ-exiting').value)
        });
        msg.textContent = response.ok ? 'Seats updated.' : whyFailed(response, 'seats');
    } catch (err) {
        msg.textContent = 'Could not reach the backend.';
    }
});

// Start trip
$('trip-form').addEventListener('submit', async e => {
    e.preventDefault();
    const msg = $('trip-msg');
    msg.textContent = 'Starting...';
    try {
        const response = await adminPost(
            `/simulation/shuttle/${$('trip-shuttle').value}/start?tripId=${$('trip-trip').value}`);
        msg.textContent = response.ok ? 'Trip started.' : whyFailed(response);
    } catch (err) {
        msg.textContent = 'Could not reach the backend.';
    }
});

// Stop trip
$('trip-stop').addEventListener('click', async () => {
    const msg = $('trip-msg');
    msg.textContent = 'Stopping...';
    try {
        const response = await adminPost(`/simulation/shuttle/${$('trip-shuttle').value}/stop`);
        msg.textContent = response.ok ? 'Trip stopped.' : whyFailed(response, 'stop');
    } catch (err) {
        msg.textContent = 'Could not reach the backend.';
    }
});

loadAdminLists();

// ---- Admin Feedback Verification Codes ----
// Only authenticated administrators can retrieve codes.

async function loadFeedbackCodes() {

    const shuttleId = $('feedback-shuttle').value;

    const message = $('feedback-codes-msg');
    const list = $('feedback-codes-list');
    const button = $('feedback-codes-load');

    // Clear previous results.
    list.replaceChildren();

    if (!shuttleId) {
        message.textContent = 'Select a shuttle first.';
        return;
    }

    message.textContent = 'Loading feedback codes...';
    button.disabled = true;

    try {

        // Retrieve completed trip sessions for this shuttle.
        const response = await fetch(
            `${API}/admin/feedback-sessions/shuttle/${encodeURIComponent(shuttleId)}`,
            {
                credentials: 'include',
                cache: 'no-store'
            }
        );

        // Only administrators should have access.
        if (response.status === 401 || response.status === 403) {
            message.textContent =
                'Please sign in as an administrator.';
            return;
        }

        if (!response.ok) {
            throw new Error('Request failed: ' + response.status);
        }

        const sessions = await response.json();

        if (!Array.isArray(sessions) || sessions.length === 0) {

            message.textContent =
                'No completed feedback sessions for this shuttle yet.';

            return;
        }

        message.textContent =
            `${sessions.length} completed feedback session(s).`;

        // Display each completed trip's verification information.
        for (const session of sessions) {

            const entry = document.createElement('div');
            entry.className = 'feedback-code-item';

            const title = document.createElement('strong');

            title.textContent =
                `Trip ${session.tripId} · Driver ${session.driverId}`;

            const code = document.createElement('div');
            code.className = 'feedback-code-value';

            code.textContent = session.expired
                ? 'Expired code'
                : session.verificationCode;

            const detail = document.createElement('div');
            detail.className = 'hint';

            const completed = new Date(session.completedAt);
            const expires = new Date(session.expiresAt);

            detail.textContent =
                `Completed: ${completed.toLocaleString()} · ` +
                `Expires: ${expires.toLocaleString()}` +
                (session.expired ? ' · Expired' : '');

            entry.append(title, code, detail);

            list.appendChild(entry);
        }

    } catch (err) {

        console.error('Could not load feedback codes:', err);

        message.textContent =
            'Could not load feedback codes. Check the backend connection.';

    } finally {

        button.disabled = false;
    }
}

// Load codes when the administrator clicks the button.
$('feedback-codes-load').addEventListener(
    'click',
    loadFeedbackCodes
);

// Clear previous codes when selecting a different shuttle.
$('feedback-shuttle').addEventListener('change', () => {

    $('feedback-codes-list').replaceChildren();

    $('feedback-codes-msg').textContent = '';
});

// ---- Admin Driver Ratings Dashboard ----
// Retrieves ratings for a selected driver.
// Only authenticated administrators can access this endpoint.
async function loadDriverRatings() {

    const driverId = $('ratings-driver').value;

    const message = $('ratings-msg');
    const button = $('ratings-load');
    const summary = $('ratings-summary');
    const list = $('ratings-list');

    // Clear previous results whenever a new request starts.
    summary.hidden = true;
    list.replaceChildren();

    if (!driverId) {
        message.textContent = 'Please select a driver.';
        return;
    }

    button.disabled = true;
    message.textContent = 'Loading driver ratings...';

    try {

        const response = await fetch(
            `${API}/admin/drivers/${encodeURIComponent(driverId)}/ratings`,
            {
                credentials: 'include',
                cache: 'no-store'
            }
        );

        if (response.status === 401 || response.status === 403) {
            message.textContent =
                'Please sign in as an administrator.';
            return;
        }

        if (!response.ok) {
            throw new Error(
                'Could not retrieve ratings: HTTP ' + response.status
            );
        }

        const data = await response.json();

        // Show the driver's average and total number of ratings.
        $('ratings-average').textContent =
            data.totalRatings > 0
                ? `${Number(data.averageRating).toFixed(1)} / 5`
                : 'No ratings';

        $('ratings-total').textContent = data.totalRatings;

        summary.hidden = false;

        if (!Array.isArray(data.ratings) ||
            data.ratings.length === 0) {

            message.textContent =
                'This driver has not received any ratings yet.';

            list.innerHTML =
                '<div class="empty">No feedback submitted yet.</div>';

            return;
        }

        message.textContent =
            `Showing ${data.totalRatings} rating(s).`;

        // Display each individual feedback record.
        data.ratings.forEach(feedback => {

            const card = document.createElement('div');
            card.className = 'driver-rating-item';

            const stars = document.createElement('div');
            stars.className = 'driver-rating-stars';

            const rating = Number(feedback.rating);

            stars.textContent =
                '★'.repeat(Math.max(0, Math.min(5, rating))) +
                '☆'.repeat(Math.max(0, 5 - Math.min(5, rating)));

            const details = document.createElement('div');
            details.className = 'hint';

            const submitted = feedback.submittedAt
                ? new Date(feedback.submittedAt).toLocaleString()
                : 'Unknown date';

            details.textContent =
                `Rating: ${rating}/5 · Submitted: ${submitted}`;

            const tripInfo = document.createElement('div');
            tripInfo.className = 'hint';

            tripInfo.textContent = feedback.sessionId != null
                ? `Feedback session: ${feedback.sessionId}`
                : 'Feedback session unavailable';

            card.append(stars, details, tripInfo);

            list.appendChild(card);
        });

    } catch (error) {

        console.error('Driver ratings request failed:', error);

        message.textContent =
            'Unable to load driver ratings. Check the backend connection.';

    } finally {

        button.disabled = false;
    }
}

// Load ratings when the administrator clicks the button.
$('ratings-load').addEventListener(
    'click',
    loadDriverRatings
);

// Clear the previous driver's ratings when changing selection.
$('ratings-driver').addEventListener('change', () => {

    $('ratings-summary').hidden = true;
    $('ratings-list').replaceChildren();
    $('ratings-msg').textContent = '';
});

// Load the available drivers when the page starts.
loadRatingsDrivers();

drawAlerts();

// Check for an existing admin session whenever the page loads.
restoreAdminSession();

/*
 * Admin Access Check
 * Revalidates the administrator session whenever
 * the Admin Tools navigation button is clicked.
 */
const adminNavButton = document.querySelector(
    '.nav-item[data-view="admin"]'
);

adminNavButton.addEventListener('click', async () => {

    // Hide the controls until Spring Security verifies the session.
    $('admin-tools').hidden = true;

    // Remove verification codes from the interface after logout.
    $('feedback-codes-list').replaceChildren();
    $('feedback-codes-msg').textContent = '';

    // Clear administrator-only ratings from the interface.
    $('ratings-summary').hidden = true;
    $('ratings-list').replaceChildren();
    $('ratings-msg').textContent = '';

    // Check the current session against the backend.
    await restoreAdminSession();
});

/*
 * Recheck the session when the user returns
 * to the browser tab.
 */
document.addEventListener('visibilitychange', async () => {

    // Only check when the tab becomes visible.
    if (document.visibilityState === 'visible') {
        await restoreAdminSession();
    }
});
