function toggleClass(obj) {
    var content = obj.nextElementSibling;
    obj.classList.toggle("active");
    if(content.style.display==="block"){
        content.style.display = "none";
    } else {
        content.style.display = "block";
    }
}