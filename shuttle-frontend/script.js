function goToApp() {
    document.getElementById('splash').style.display = 'none';
}


// ---- navbar ----
(function () {
    var toggle = document.getElementById('nav-toggle');
    var menu = document.getElementById('nav-menu');
    var items = Array.prototype.slice.call(menu.querySelectorAll('.nav-item'));

    // Open or close the menu and keep the button's icon and labels in sync.
    function setOpen(open) {
        menu.classList.toggle('open', open);
        toggle.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
        if (!open) items.forEach(function (p) { p.removeAttribute('aria-pressed'); });
    }

    toggle.addEventListener('click', function () {
        setOpen(!menu.classList.contains('open'));
    });

    // When a menu button is clicked, mark it and announce which screen was picked.
    items.forEach(function (item) {
        item.addEventListener('click', function () {
            items.forEach(function (p) { p.setAttribute('aria-pressed', String(p === item)); });
            document.dispatchEvent(new CustomEvent('navselect', { detail: { view: item.getAttribute('data-view') } }));
        });
    });

    // Escape closes the menu.
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') setOpen(false);
    });
})();


(function () {
    var panel = document.getElementById('panel');
    var menu = document.getElementById('nav-menu');

    // ---- Sample data. Replace with real data from the backend later. ----
    var STOPS = {
        lirr: 'Farmingdale LIRR',
        nold: 'Nold Hall',
        aviation: 'Aviation Center'
    };

    var ROUTES = [
        { key: 'lirr', name: 'Farmingdale LIRR', color: '#7B40BA' },
        { key: 'nold', name: 'Nold Hall', color: '#4081BA' },
        { key: 'aviation', name: 'Aviation Center', color: '#BA8140' }
    ];

    var SCHEDULE = [
        { type: 'Off Peak', crowd: 1, time: '12:00 - 12:30 PM', eta: '10 mins', seats: 10,
            stops: [['Stop 1', '12:00 PM', 'On Time'], ['Stop 2', '12:30 PM', 'Delayed']] },
        { type: 'Peak', crowd: 3, time: '12:30 - 1:00 PM', eta: '40 mins', seats: 4,
            stops: [['Stop 1', '12:30 PM', 'On Time'], ['Stop 2', '1:00 PM', 'On Time']] },
        { type: 'Off Peak', crowd: 2, time: '1:00 - 1:30 PM', eta: '70 mins', seats: 7,
            stops: [['Stop 1', '1:00 PM', 'On Time'], ['Stop 2', '1:30 PM', 'On Time']] }
    ];

    // ---- Small helpers ----
    function $(id) {
        return document.getElementById(id);
    }

    // makes text safe to put inside HTML
    function esc(text) {
        var map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
        return String(text).replace(/[&<>"']/g, function (c) { return map[c]; });
    }

    function timeNow(ms) {
        return new Date(ms || Date.now()).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    }


    // ---- Show the panel that matches the menu button ----
    function showView(id) {
        var views = panel.querySelectorAll('.view');
        for (var i = 0; i < views.length; i++) {
            views[i].classList.toggle('active', views[i].id === id);
        }
        panel.classList.add('has-content');
    }

    document.addEventListener('navselect', function (e) {
        showView(e.detail.view);
    });

    // when the menu closes, close the panel too
    new MutationObserver(function () {
        if (!menu.classList.contains('open')) {
            panel.classList.remove('has-content');
        }
    }).observe(menu, { attributes: true, attributeFilter: ['class'] });



    // View Routes: one toggle switch per route
    var toggleBox = $('route-toggles');

    ROUTES.forEach(function (route) {
        var row = document.createElement('div');
        row.className = 'route-row';
        row.innerHTML =
            '<span class="toggle">' +
            '<input type="checkbox" id="toggle-' + route.key + '" data-route="' + route.key + '" aria-label="Show ' + route.name + ' route">' +
            '<span class="track"></span><span class="knob"></span>' +
            '</span>' +
            '<label for="toggle-' + route.key + '">' +
            '<span class="dot" style="background:' + route.color + '"></span>' + route.name +
            '</label>';
        toggleBox.appendChild(row);
    });

    // Tells the rest of the app which routes are on.
    // Later, the map code can listen for it:
    //   document.addEventListener('routeschange', function (e) { console.log(e.detail.routes); });
    toggleBox.addEventListener('change', function () {
        var on = [];
        var boxes = toggleBox.querySelectorAll('input:checked');
        for (var i = 0; i < boxes.length; i++) {
            on.push(boxes[i].getAttribute('data-route'));
        }
        document.dispatchEvent(new CustomEvent('routeschange', { detail: { routes: on } }));
    });


    // ==========================================================
    // Search Routes: pick From and To, see times, open More Info
    // ==========================================================
    var fromSelect = $('search-from');
    var toSelect = $('search-to');
    var resultsBox = $('search-results');
    var searchForm = $('search-form');
    var detailBox = $('search-detail');

    function fillStops(select, label) {
        var html = '<option value="">' + label + '</option>';
        for (var key in STOPS) {
            html += '<option value="' + key + '">' + STOPS[key] + '</option>';
        }
        select.innerHTML = html;
    }
    fillStops(fromSelect, 'From');
    fillStops(toSelect, 'To');

    function crowdText(level) {
        var dots = '';
        for (var i = 1; i <= 4; i++) {
            dots += i <= level ? '●' : '○';   // filled and empty circles
        }
        return '<span class="crowd" role="img" aria-label="Crowd level ' + level + ' of 4">' + dots + '</span>';
    }

    function runSearch() {
        var from = fromSelect.value;
        var to = toSelect.value;

        if (!from || !to) {
            resultsBox.innerHTML = '<div class="empty">Search results will appear here</div>';
            return;
        }
        if (from === to) {
            resultsBox.innerHTML = '<div class="empty">Pick two different stops.</div>';
            return;
        }

        var html = '<p class="hint">Select More Info to see stop details.</p>';
        SCHEDULE.forEach(function (trip, i) {
            html +=
                '<div class="result">' +
                '<span class="type ' + (trip.type === 'Peak' ? 'peak' : '') + '">' + trip.type + crowdText(trip.crowd) + '</span>' +
                '<span class="time">' + trip.time + '</span>' +
                '<button class="panel-link" type="button" data-trip="' + i + '">More Info &#9656;</button>' +
                '<span class="eta">' + trip.eta + '</span>' +
                '</div>';
        });
        resultsBox.innerHTML = html;
    }

    fromSelect.addEventListener('change', runSearch);
    toSelect.addEventListener('change', runSearch);

    // More Info: show the stop list for one trip
    resultsBox.addEventListener('click', function (e) {
        var button = e.target.closest('button[data-trip]');
        if (!button) return;

        var trip = SCHEDULE[Number(button.getAttribute('data-trip'))];
        var rows = '';
        trip.stops.forEach(function (stop) {
            var cls = stop[2] === 'On Time' ? 'on-time' : '';
            rows += '<tr><td>' + stop[0] + '</td><td>' + stop[1] + '</td><td class="' + cls + '">' + stop[2] + '</td></tr>';
        });

        detailBox.innerHTML =
            '<div class="detail">' +
            '<button class="panel-link" type="button" id="detail-back">&#9664; Back</button>' +
            '<table><thead><tr><th>Stops</th><th>Time</th><th>Status</th></tr></thead><tbody>' + rows + '</tbody></table>' +
            '<div class="seats">Available Seats: <strong>' + trip.seats + '</strong></div>' +
            '</div>';
        detailBox.hidden = false;
        searchForm.hidden = true;
        $('detail-back').focus();
    });

    detailBox.addEventListener('click', function (e) {
        if (e.target.id !== 'detail-back') return;
        detailBox.hidden = true;
        searchForm.hidden = false;
    });


    // ==========================================================
    // Feedback
    // ==========================================================
    $('feedback-form').addEventListener('submit', function (e) {
        e.preventDefault();
        // TODO: send $('feedback-text').value to the backend when it exists
        $('feedback-msg').textContent = 'Thanks. Your feedback was sent.';
        $('feedback-text').value = '';
    });


    // ==========================================================
    // Admin Tools: sign in, post alerts, remove alerts
    // ==========================================================
    var ALERT_KEY = 'fst-alerts';
    var signedIn = false;

    // Alerts are saved in this browser only, so the demo works.
    // TODO: when the backend exists, load and save alerts there so every rider sees them.
    function loadAlerts() {
        try {
            var saved = JSON.parse(localStorage.getItem(ALERT_KEY));
            return Array.isArray(saved) ? saved : [];
        } catch (err) {
            return [];
        }
    }

    function saveAlerts() {
        try {
            localStorage.setItem(ALERT_KEY, JSON.stringify(alerts));
        } catch (err) {
            // storage is blocked, the alerts just won't be remembered
        }
    }

    var alerts = loadAlerts();

    // TODO: replace this with a real login request to the backend.
    // For now any username and password works, so this does NOT protect anything.

    function checkLogin(user, pass) {
        return user !== '' && pass !== '';
    }

    function showAdminState() {
        $('admin-login').hidden = signedIn;
        $('admin-tools').hidden = !signedIn;
        if (signedIn) drawAlerts();
    }

    function drawAlerts() {
        var box = $('admin-alerts');
        if (!alerts.length) {
            box.innerHTML = '<div class="empty">No active alerts.</div>';
            return;
        }
        var html = '';
        alerts.forEach(function (alert) {
            html +=
                '<div class="alert-item">' +
                '<span class="text">' + esc(alert.text) + '<span class="when">Posted ' + timeNow(alert.id) + '</span></span>' +
                '<button class="remove" type="button" data-id="' + alert.id + '" aria-label="Remove this alert">Remove</button>' +
                '</div>';
        });
        box.innerHTML = html;
    }

    $('admin-login').addEventListener('submit', function (e) {
        e.preventDefault();
        var user = $('admin-user').value.trim();
        var pass = $('admin-pass').value;

        if (!checkLogin(user, pass)) {
            $('login-msg').textContent = 'Wrong username or password. Try again.';
            return;
        }
        signedIn = true;
        $('admin-pass').value = '';
        $('login-msg').textContent = '';
        $('admin-msg').textContent = '';
        showAdminState();
        $('alert-text').focus();
    });

    $('alert-form').addEventListener('submit', function (e) {
        e.preventDefault();
        var text = $('alert-text').value.trim();
        if (!text) return;

        alerts.push({ id: Date.now(), text: text });
        saveAlerts();
        $('alert-text').value = '';
        drawAlerts();
        $('admin-msg').textContent = 'Alert posted.';
        // lets the rest of the app (like a notification bell) know the list changed
        document.dispatchEvent(new CustomEvent('alertschange', { detail: { alerts: alerts } }));
    });

    $('admin-alerts').addEventListener('click', function (e) {
        var button = e.target.closest('button[data-id]');
        if (!button) return;

        var id = button.getAttribute('data-id');
        alerts = alerts.filter(function (alert) { return String(alert.id) !== id; });
        saveAlerts();
        drawAlerts();
        $('admin-msg').textContent = 'Alert removed.';
        document.dispatchEvent(new CustomEvent('alertschange', { detail: { alerts: alerts } }));
    });

    $('admin-signout').addEventListener('click', function () {
        signedIn = false;
        $('admin-msg').textContent = '';
        showAdminState();
        $('admin-user').focus();
    });

    showAdminState();
})();