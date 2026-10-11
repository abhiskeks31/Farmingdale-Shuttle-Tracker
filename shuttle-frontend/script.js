// Get Started button: hide the splash screen
function goToApp() {
    document.getElementById('splash').style.display = 'none';
}

const $ = id => document.getElementById(id);


// ---- sample data (replace with real data from the backend later) ----

const STOPS = ['Farmingdale LIRR', 'Nold Hall', 'Aviation Center'];

const ROUTES = [
    { name: 'Farmingdale LIRR', color: '#7B40BA' },
    { name: 'Nold Hall', color: '#4081BA' },
    { name: 'Aviation Center', color: '#BA8140' }
];

const SCHEDULE = [
    { type: 'Off Peak', crowd: 1, time: '12:00 - 12:30 PM', eta: '10 mins', seats: 10,
        stops: [['Stop 1', '12:00 PM', 'On Time'], ['Stop 2', '12:30 PM', 'Delayed']] },
    { type: 'Peak', crowd: 3, time: '12:30 - 1:00 PM', eta: '40 mins', seats: 4,
        stops: [['Stop 1', '12:30 PM', 'On Time'], ['Stop 2', '1:00 PM', 'On Time']] },
    { type: 'Off Peak', crowd: 2, time: '1:00 - 1:30 PM', eta: '70 mins', seats: 7,
        stops: [['Stop 1', '1:00 PM', 'On Time'], ['Stop 2', '1:30 PM', 'On Time']] }
];



// makes text safe to put inside HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(ms) {
    return new Date(ms).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
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

// a menu button highlights itself and shows its own panel
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

ROUTES.forEach((route, i) => {
    const row = document.createElement('div');
    row.className = 'route-row';
    row.innerHTML = `
        <span class="toggle">
            <input type="checkbox" id="route-${i}" aria-label="Show ${route.name} route">
            <span class="track"></span><span class="knob"></span>
        </span>
        <label for="route-${i}"><span class="dot" style="background:${route.color}"></span>${route.name}</label>`;
    $('route-toggles').appendChild(row);
});


// ---- Search Routes ----

const fromSelect = $('search-from');
const toSelect = $('search-to');
const resultsBox = $('search-results');
const searchForm = $('search-form');
const detailBox = $('search-detail');

function fillStops(select, label) {
    const options = STOPS.map(stop => `<option>${stop}</option>`).join('');
    select.innerHTML = `<option value="">${label}</option>${options}`;
}
fillStops(fromSelect, 'From');
fillStops(toSelect, 'To');

// filled and empty circles, like ●●○○
function crowdHtml(level) {
    const dots = '●'.repeat(level) + '○'.repeat(4 - level);
    return `<span class="crowd" role="img" aria-label="Crowd level ${level} of 4">${dots}</span>`;
}

function showResults() {
    if (!fromSelect.value || !toSelect.value) {
        resultsBox.innerHTML = '<div class="empty">Search results will appear here</div>';
    } else if (fromSelect.value === toSelect.value) {
        resultsBox.innerHTML = '<div class="empty">Pick two different stops.</div>';
    } else {
        let html = '<p class="hint">Select More Info to see stop details.</p>';
        SCHEDULE.forEach((trip, i) => {
            html += `
                <div class="result">
                    <span class="type ${trip.type === 'Peak' ? 'peak' : ''}">${trip.type}${crowdHtml(trip.crowd)}</span>
                    <span class="time">${trip.time}</span>
                    <button class="panel-link" type="button" data-trip="${i}">More Info &#9656;</button>
                    <span class="eta">${trip.eta}</span>
                </div>`;
        });
        resultsBox.innerHTML = html;
    }
}

fromSelect.addEventListener('change', showResults);
toSelect.addEventListener('change', showResults);

// More Info: swap the results for the stop list of that trip
resultsBox.addEventListener('click', e => {
    const button = e.target.closest('button[data-trip]');
    if (!button) return;

    const trip = SCHEDULE[button.dataset.trip];
    const rows = trip.stops.map(([name, time, status]) => `
        <tr><td>${name}</td><td>${time}</td><td class="${status === 'On Time' ? 'on-time' : ''}">${status}</td></tr>`).join('');

    detailBox.innerHTML = `
        <div class="detail">
            <button class="panel-link" type="button" id="detail-back">&#9664; Back</button>
            <table>
                <thead><tr><th>Stops</th><th>Time</th><th>Status</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
            <div class="seats">Available Seats: <strong>${trip.seats}</strong></div>
        </div>`;

    $('detail-back').addEventListener('click', () => {
        detailBox.hidden = true;
        searchForm.hidden = false;
    });

    detailBox.hidden = false;
    searchForm.hidden = true;
    $('detail-back').focus();
});


// ---- Feedback ----

$('feedback-form').addEventListener('submit', e => {
    e.preventDefault();
    // TODO: send the feedback text to the backend
    $('feedback-msg').textContent = 'Thanks. Your feedback was sent.';
    $('feedback-text').value = '';
});


// ---- Alerts and Admin Tools ----

// For now alerts are saved in this browser only, so the demo works.
// TODO: load and save alerts through the backend so every rider sees them.
let alerts = [];
try {
    const saved = JSON.parse(localStorage.getItem('fst-alerts'));
    if (Array.isArray(saved)) alerts = saved;
} catch (err) {
    // storage is blocked, start with no alerts
}

function saveAlerts() {
    try {
        localStorage.setItem('fst-alerts', JSON.stringify(alerts));
    } catch (err) {
        // storage is blocked, the alerts just won't be remembered
    }
}

// draws the alert list in the Alert panel (riders) and in Admin Tools
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
}

$('admin-login').addEventListener('submit', e => {
    e.preventDefault();
    // TODO: check the username and password with the backend.
    // For now any username and password works, so this does NOT protect anything.
    $('admin-pass').value = '';
    $('admin-msg').textContent = '';
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
    saveAlerts();
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