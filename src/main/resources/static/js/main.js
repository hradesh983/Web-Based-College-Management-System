document.addEventListener("DOMContentLoaded", function () {
    const toggleBtn = document.getElementById("sidebarToggle");
    const sidebar = document.getElementById("sidebar");

    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener("click", function () {
            sidebar.classList.toggle("show");
        });
    }
});

function confirmDelete(event, entityName) {
    if (!confirm("Are you sure you want to delete this " + entityName + "? This action cannot be undone.")) {
        event.preventDefault();
        return false;
    }
    return true;
}
