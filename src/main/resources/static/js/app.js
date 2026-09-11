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
});

// Helper to close modals easily
function closeModal() {
    const modalContainer = document.getElementById('modal-container');
    if (modalContainer) {
        modalContainer.innerHTML = '';
    }
}
