// Özerler Mermer ERP - Core Client Application JS
document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Lucide icons
    if (window.lucide) {
        window.lucide.createIcons();
    }

    // 2. Configure HTMX to automatically inject CSRF token from cookie or meta tag
    document.body.addEventListener('htmx:configRequest', (event) => {
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');

        if (csrfHeader && csrfToken) {
            event.detail.headers[csrfHeader] = csrfToken;
        } else {
            // Read from XSRF-TOKEN cookie if meta is not present
            const match = document.cookie.match(new RegExp('(^| )XSRF-TOKEN=([^;]+)'));
            if (match) {
                event.detail.headers['X-XSRF-TOKEN'] = decodeURIComponent(match[2]);
            }
        }
    });

    // 3. Re-initialize Lucide icons after any HTMX swap
    document.body.addEventListener('htmx:afterSettle', () => {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    });

    document.body.addEventListener('userSaved', () => {
        closeModal();
        if (typeof reloadUsersGrid === 'function') {
            reloadUsersGrid();
        }
    });

    // 4. Update port indicator dynamically based on current location
    const port = window.location.port || (window.location.protocol === 'https:' ? '443' : '80');
    document.querySelectorAll('.app-port-text').forEach(el => {
        el.textContent = port;
    });
});

function closeModal() {
    const modalContainer = document.getElementById('modal-container');
    if (modalContainer) {
        modalContainer.innerHTML = '';
    }
}

function attachTabulatorPagingAnimation(table) {
    if (!table || !table.element) {
        return;
    }
    const root = table.element;
    table.on("dataLoading", () => {
        root.classList.add("is-paging");
    });
    table.on("dataLoaded", () => {
        window.setTimeout(() => root.classList.remove("is-paging"), 40);
    });
    table.on("dataLoadError", () => {
        root.classList.remove("is-paging");
    });
}

function camelizeTabulatorRows(response) {
    if (response && Array.isArray(response.data)) {
        response.data.forEach((item) => {
            Object.keys(item).forEach((key) => {
                const camel = key.replace(/_([a-z0-9])/g, (_, letter) => letter.toUpperCase());
                if (!(camel in item)) {
                    item[camel] = item[key];
                }
            });
        });
    }
    return response;
}

function applyTabulatorTotal(tableId, response) {
    const badge = document.querySelector(`[data-grid-total="${tableId}"]`);
    if (badge && response && typeof response.total === "number") {
        badge.textContent = String(response.total);
    }
    return response;
}

function toAsciiTurkishFilename(filename) {
    if (!filename) {
        return "indirilen";
    }
    const ascii = filename
        .replaceAll("ç", "c").replaceAll("Ç", "C")
        .replaceAll("ğ", "g").replaceAll("Ğ", "G")
        .replaceAll("ı", "i").replaceAll("İ", "I")
        .replaceAll("ö", "o").replaceAll("Ö", "O")
        .replaceAll("ş", "s").replaceAll("Ş", "S")
        .replaceAll("ü", "u").replaceAll("Ü", "U")
        .toLowerCase()
        .replace(/[^a-z0-9._-]+/g, "_")
        .replace(/^_+|_+$/g, "");
    return ascii || "indirilen";
}

function downloadTableCsv(table, filename) {
    if (!table) {
        return;
    }
    table.download("csv", toAsciiTurkishFilename(filename));
}

function bindGridSearch(table, inputId) {
    const input = document.getElementById(inputId);
    if (!table || !input) {
        return;
    }
    const form = input.closest("form");
    let debounceTimer;
    input.addEventListener("input", () => {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => table.replaceData(), 300);
    });
    if (form) {
        form.addEventListener("submit", (event) => {
            event.preventDefault();
            clearTimeout(debounceTimer);
            table.replaceData();
        });
    }
}
