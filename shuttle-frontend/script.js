
// will  change this to the name of the main page
var nextPage = "app.html";

function goToApp() {
    window.location.href = nextPage;
}

// go to the main page automatically after 5 seconds
setTimeout(goToApp, 5000);
