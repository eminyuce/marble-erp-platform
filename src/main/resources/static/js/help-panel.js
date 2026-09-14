/**
 * Shared right-side slide-over help panel.
 *
 * Register before Alpine loads (alpine:init). Any page can open it with:
 *
 *   <button @click="$dispatch('open-help', { pageKey: 'change-password' })">Yardım</button>
 *   window.openHelpPanel('change-password')
 *
 * GET  /api/help/{pageKey}
 * POST /api/help/{pageKey}/feedback  { "helpful": true|false }
 */
const HELP_PANEL_FOCUSABLE = 'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])';

function helpCsrfHeaders() {
    const headers = { 'Accept': 'application/json' };
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    } else {
        const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
        if (match) {
            headers['X-XSRF-TOKEN'] = decodeURIComponent(match[1]);
        }
    }
    return headers;
}

function formatHelpDate(value) {
    if (!value) {
        return '';
    }
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
        return String(value);
    }
    return parsed.toLocaleDateString('tr-TR');
}

function renderHelpBody(body, format) {
    if (!body) {
        return '';
    }
    if (format === 'markdown') {
        return renderSimpleMarkdown(body);
    }
    return body;
}

function renderSimpleMarkdown(source) {
    const escaped = source
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
    const lines = escaped.split(/\r?\n/);
    let html = '';
    let inList = false;
    const flushList = () => {
        if (inList) {
            html += '</ul>';
            inList = false;
        }
    };
    const inline = (text) => text
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\*(.+?)\*/g, '<em>$1</em>');
    for (const raw of lines) {
        const line = raw.trim();
        if (!line) {
            flushList();
            continue;
        }
        if (line.startsWith('### ')) {
            flushList();
            html += '<h4>' + inline(line.slice(4)) + '</h4>';
        } else if (line.startsWith('## ')) {
            flushList();
            html += '<h3>' + inline(line.slice(3)) + '</h3>';
        } else if (line.startsWith('# ')) {
            flushList();
            html += '<h3>' + inline(line.slice(2)) + '</h3>';
        } else if (line.startsWith('- ') || line.startsWith('* ')) {
            if (!inList) {
                html += '<ul class="help-panel-list">';
                inList = true;
            }
            html += '<li>' + inline(line.slice(2)) + '</li>';
        } else {
            flushList();
            html += '<p>' + inline(line) + '</p>';
        }
    }
    flushList();
    return html;
}

const HELP_PATH_PREFIXES = [
    ['/account/change-password', 'change-password'],
    ['/production/slabs', 'slabs'],
    ['/admin/settings', 'settings'],
    ['/admin/users', 'users'],
    ['/admin/dashboard', 'dashboard'],
    ['/production', 'production'],
    ['/workshop', 'workshop'],
    ['/projects', 'projects'],
    ['/procurement', 'procurement'],
    ['/sales', 'sales'],
    ['/blocks', 'blocks'],
    ['/genealogy', 'genealogy'],
    ['/reports', 'reports'],
    ['/costs', 'costs']
].sort((left, right) => right[0].length - left[0].length);

function pageKeyFromPath(pathname) {
    const path = (pathname || '').split('?')[0].replace(/\/+$/, '') || '/';
    for (const [prefix, key] of HELP_PATH_PREFIXES) {
        if (path === prefix || path.startsWith(prefix + '/')) {
            return key;
        }
    }
    return '';
}

function createHelpPanel() {
    return {
        open: false,
        loading: false,
        error: '',
        pageKey: '',
        title: 'Yardım',
        bodyHtml: '',
        lastUpdated: '',
        feedback: null,
        feedbackBusy: false,
        previousFocus: null,

        init() {
            window.openHelpPanel = (pageKey) => this.openHelp(pageKey);
        },

        defaultPageKey() {
            return document.body?.dataset?.helpPageKey || pageKeyFromPath(window.location.pathname) || '';
        },

        openFromEvent(event) {
            this.openHelp(event?.detail?.pageKey || this.defaultPageKey());
        },

        openHelp(pageKey) {
            const key = (pageKey || this.defaultPageKey() || '').trim();
            if (!key) {
                this.pageKey = '';
                this.title = 'Yardım';
                this.bodyHtml = '';
                this.error = 'Bu sayfa için yardım içeriği bulunamadı.';
                this.lastUpdated = '';
                this.feedback = null;
                this.showPanel();
                return;
            }
            this.pageKey = key;
            this.loading = true;
            this.error = '';
            this.bodyHtml = '';
            this.lastUpdated = '';
            this.feedback = null;
            this.showPanel();
            this.fetchHelp(key);
        },

        showPanel() {
            this.previousFocus = document.activeElement;
            this.open = true;
            document.body.classList.add('help-panel-open');
            this.$nextTick(() => {
                if (window.lucide) {
                    window.lucide.createIcons();
                }
                this.$refs.closeBtn?.focus();
            });
        },

        closeHelp() {
            if (!this.open) {
                return;
            }
            this.open = false;
            document.body.classList.remove('help-panel-open');
            const restore = this.previousFocus;
            this.previousFocus = null;
            if (restore && typeof restore.focus === 'function' && document.contains(restore)) {
                restore.focus();
            }
        },

        async fetchHelp(pageKey) {
            try {
                const response = await fetch('/api/help/' + encodeURIComponent(pageKey), {
                    headers: helpCsrfHeaders()
                });
                if (response.status === 404) {
                    this.error = 'Bu sayfa için yardım içeriği bulunamadı.';
                    this.title = 'Yardım';
                    return;
                }
                if (!response.ok) {
                    this.error = 'Yardım içeriği yüklenemedi.';
                    return;
                }
                const data = await response.json();
                this.title = data.title || 'Yardım';
                this.bodyHtml = renderHelpBody(data.body, data.format);
                this.lastUpdated = formatHelpDate(data.lastUpdated);
            } catch (err) {
                this.error = 'Yardım içeriği yüklenemedi.';
            } finally {
                this.loading = false;
                this.$nextTick(() => {
                    if (window.lucide) {
                        window.lucide.createIcons();
                    }
                });
            }
        },

        async sendFeedback(helpful) {
            if (!this.pageKey || this.feedbackBusy || this.feedback) {
                return;
            }
            this.feedbackBusy = true;
            try {
                const headers = helpCsrfHeaders();
                headers['Content-Type'] = 'application/json';
                const response = await fetch('/api/help/' + encodeURIComponent(this.pageKey) + '/feedback', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ helpful })
                });
                if (!response.ok) {
                    return;
                }
                this.feedback = helpful ? 'up' : 'down';
            } finally {
                this.feedbackBusy = false;
            }
        },

        onKeydown(event) {
            if (!this.open) {
                return;
            }
            if (event.key === 'Escape') {
                event.preventDefault();
                this.closeHelp();
                return;
            }
            if (event.key === 'Tab') {
                this.keepFocusInside(event);
            }
        },

        keepFocusInside(event) {
            const panel = this.$refs.panel;
            if (!panel) {
                return;
            }
            const nodes = Array.from(panel.querySelectorAll(HELP_PANEL_FOCUSABLE))
                .filter((el) => !el.hasAttribute('disabled') && el.offsetParent !== null);
            if (nodes.length === 0) {
                event.preventDefault();
                panel.focus();
                return;
            }
            const first = nodes[0];
            const last = nodes[nodes.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        }
    };
}

document.addEventListener('alpine:init', () => {
    Alpine.data('helpPanel', createHelpPanel);
});
