// the map, made with Leaflet and OpenStreetMap
let map = null;

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
}