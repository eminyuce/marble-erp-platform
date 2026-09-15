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

function erpGridDefaults() {
    return {
        layout: "fitColumns",
        autoResize: false,
        renderVertical: "basic",
        responsiveLayout: "collapse",
        responsiveLayoutCollapseStartOpen: false,
        minHeight: 180,
        placeholder: "Kayıt bulunamadı.",
        locale: "tr",
        langs: {
            tr: {
                data: {
                    loading: "Yükleniyor",
                    error: "Veri yüklenemedi"
                },
                pagination: {
                    page_size: "Sayfa boyutu",
                    page_title: "Sayfa",
                    first: "İlk",
                    first_title: "İlk sayfa",
                    last: "Son",
                    last_title: "Son sayfa",
                    prev: "Önceki",
                    prev_title: "Önceki sayfa",
                    next: "Sonraki",
                    next_title: "Sonraki sayfa",
                    all: "Tümü",
                    counter: {
                        showing: "Gösterilen",
                        of: "/",
                        rows: "kayıt",
                        pages: "sayfa"
                    }
                }
            }
        }
    };
}

function erpGridAjaxUrl(url, params, searchInputId, extraQuery) {
    const searchVal = document.getElementById(searchInputId)?.value || "";
    const page = Number(params && params.page) > 0 ? params.page : 1;
    const size = Number(params && params.size) > 0 ? params.size : (window.ERP_GRID_PAGE_SIZE || 25);
    let sorterField = "";
    let sorterDir = "";
    if (params && Array.isArray(params.sorters) && params.sorters.length > 0) {
        sorterField = params.sorters[0].field || "";
        sorterDir = params.sorters[0].dir || "";
    }
    let query = `${url}?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}&search=${encodeURIComponent(searchVal)}&sortField=${encodeURIComponent(sorterField)}&sortDir=${encodeURIComponent(sorterDir)}`;
    if (typeof extraQuery === "function") {
        query += extraQuery() || "";
    } else if (extraQuery) {
        query += extraQuery;
    }
    return query;
}

function emptyTabulatorResponse() {
    return {data: [], last_page: 1, total: 0};
}

function erpGridAjaxResponse(tableId, response) {
    let payload = response;
    if (typeof payload === "string") {
        try {
            payload = JSON.parse(payload);
        } catch (e) {
            return emptyTabulatorResponse();
        }
    }
    const target = camelizeTabulatorRows(payload);
    if (!target || !Array.isArray(target.data)) {
        return emptyTabulatorResponse();
    }
    if (typeof target.last_page !== "number" || target.last_page < 1) {
        target.last_page = 1;
    }
    return applyTabulatorTotal(tableId, target);
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

    let resizeTimer;
    window.addEventListener("resize", () => {
        clearTimeout(resizeTimer);
        resizeTimer = window.setTimeout(() => {
            if (table.element && document.body.contains(table.element)) {
                table.redraw(true);
            }
        }, 200);
    });
}

function camelizeTabulatorRows(response) {
    const target = (response && response.response && response.response.body) ? response.response.body : response;
    if (target && Array.isArray(target.data)) {
        target.data.forEach((item) => {
            Object.keys(item).forEach((key) => {
                const camel = key.replace(/_([a-z0-9])/g, (_, letter) => letter.toUpperCase());
                if (!(camel in item)) {
                    item[camel] = item[key];
                }
            });
        });
    }
    return target;
}

function applyTabulatorTotal(tableId, response) {
    const target = camelizeTabulatorRows(response);
    const badge = document.querySelector(`[data-grid-total="${tableId}"]`);
    if (badge && target && typeof target.total === "number") {
        badge.textContent = String(target.total);
    }
    return target;
}

function gridText(value) {
    return value === undefined || value === null || value === "" ? "—" : String(value);
}

function gridNumber(value, format) {
    if (value === undefined || value === null || value === "") {
        return "—";
    }
    const n = Number(value);
    if (!Number.isFinite(n)) {
        return "—";
    }
    return typeof format === "function" ? format(n) : String(n);
}

function gridMoney(value) {
    const amount = gridNumber(value, (n) => n.toLocaleString("tr-TR"));
    return amount === "—" ? "—" : amount + " TL";
}

function gridArea(value) {
    const amount = gridNumber(value, (n) => n.toFixed(2));
    return amount === "—" ? "—" : amount + " m²";
}

function erpStatusBadge(status, label) {
    const code = status || "";
    let badge = "bg-amber-100 text-amber-800";
    if (code === "COMPLETED" || code === "AVAILABLE" || code === "READY") {
        badge = "bg-emerald-100 text-emerald-800";
    } else if (code === "IN_PROGRESS" || code === "IN_CUTTING" || code === "PACKED") {
        badge = "bg-blue-100 text-blue-800";
    } else if (code === "PLANNED" || code === "RESERVED") {
        badge = "bg-slate-100 text-slate-700";
    } else if (code === "CANCELLED" || code === "SCRAPPED") {
        badge = "bg-rose-100 text-rose-800";
    } else if (code === "INSTALLED" || code === "DELIVERED") {
        badge = "bg-indigo-100 text-indigo-800";
    }
    return `<span class="px-2.5 py-1 rounded-full text-xs font-semibold ${badge}">● ${gridText(label || status)}</span>`;
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

function padExportTimeUnit(value) {
    return String(value).padStart(2, "0");
}

function formatExportTimestamp(date) {
    var d = date || new Date();
    return d.getFullYear()
        + "-" + padExportTimeUnit(d.getMonth() + 1)
        + "-" + padExportTimeUnit(d.getDate())
        + "_" + padExportTimeUnit(d.getHours())
        + "-" + padExportTimeUnit(d.getMinutes())
        + "-" + padExportTimeUnit(d.getSeconds());
}

function buildExportFilename(entityName, extension, date) {
    var stem = toAsciiTurkishFilename(entityName).replace(/\.\w+$/, "") || "indirilen";
    var ext = String(extension || "").replace(/^\.+/, "").toLowerCase();
    return stem + "_" + formatExportTimestamp(date) + (ext ? "." + ext : "");
}

function resolveExportTable(table) {
    if (table && typeof table.download === "function") {
        return table;
    }
    if (typeof table !== "string" || !table) {
        return null;
    }
    var named = window[table];
    if (named && typeof named.download === "function") {
        return named;
    }
    var selector = table.charAt(0) === "#" ? table : "#" + table;
    if (window.Tabulator && typeof Tabulator.findTable === "function") {
        var found = Tabulator.findTable(selector);
        if (found && found[0] && typeof found[0].download === "function") {
            return found[0];
        }
    }
    var element = document.querySelector(selector);
    if (element && element.tabulator && typeof element.tabulator.download === "function") {
        return element.tabulator;
    }
    return null;
}

function downloadTableCsv(table, filename) {
    var resolved = resolveExportTable(table);
    if (!resolved) {
        return;
    }
    resolved.download("csv", buildExportFilename(filename, "csv"));
}

function downloadTable(table, baseName, format) {
    var resolved = resolveExportTable(table);
    if (!resolved) {
        return;
    }
    if (format === 'xlsx') {
        resolved.download("xlsx", buildExportFilename(baseName, "xlsx"), {sheetName: "Veri"});
    } else {
        resolved.download("csv", buildExportFilename(baseName, "csv"));
    }
}

function exportDropdownHtml(tableVar, baseName) {
    return '<div x-data="{ open: false }" class="relative">' +
        '<button @click="open = !open" type="button" class="erp-btn-secondary">' +
        '<i data-lucide="download"></i>' +
        '<span>D\u0131\u015fa Aktar</span>' +
        '<i data-lucide="chevron-down" class="w-3 h-3"></i>' +
        '</button>' +
        '<div x-show="open" @click.outside="open = false" x-transition ' +
        'class="absolute right-0 mt-1 w-52 bg-white rounded-xl shadow-lg border border-slate-200 py-1.5 z-50" style="display:none;">' +
        '<button @click="downloadTable(' + tableVar + ', \'' + baseName + '\', \'csv\'); open = false" ' +
        'type="button" class="w-full px-3 py-2 text-left text-xs hover:bg-slate-50 flex items-center gap-2.5 cursor-pointer">' +
        '<i data-lucide="file-text" class="w-4 h-4 text-slate-500"></i>' +
        '<span><strong>CSV</strong> <span class="text-slate-400">(.csv)</span></span>' +
        '</button>' +
        '<button @click="downloadTable(' + tableVar + ', \'' + baseName + '\', \'xlsx\'); open = false" ' +
        'type="button" class="w-full px-3 py-2 text-left text-xs hover:bg-slate-50 flex items-center gap-2.5 cursor-pointer">' +
        '<i data-lucide="file-spreadsheet" class="w-4 h-4 text-emerald-600"></i>' +
        '<span><strong>Excel</strong> <span class="text-slate-400">(.xlsx)</span></span>' +
        '</button>' +
        '</div>' +
        '</div>';
}

function erpResponsiveCollapseColumn() {
    return {
        title: "",
        formatter: "responsiveCollapse",
        width: 46,
        minWidth: 46,
        hozAlign: "center",
        headerHozAlign: "center",
        resizable: false,
        headerSort: false,
        responsive: 0,
        download: false
    };
}

function gridActionsHtml(items) {
    var menu = items.map(function (item) {
        if (item.divider) return '<hr class="grid-actions-divider">';
        var cls = item.danger ? 'grid-actions-item grid-actions-item--danger' : 'grid-actions-item';
        var tag = item.href ? 'a' : 'button';
        var attrs = '';
        if (item.href) attrs += ' href="' + item.href + '"';
        if (item.target) attrs += ' target="' + item.target + '"';
        if (item.onclick) attrs += ' onclick="' + item.onclick + '"';
        if (item.htmx) attrs += ' hx-get="' + item.htmx + '" hx-target="#modal-container"';
        return '<' + tag + ' class="' + cls + '"' + attrs + '>' +
            '<i data-lucide="' + item.icon + '" class="w-4 h-4"></i> ' + item.label +
            '</' + tag + '>';
    }).join('');

    return '<div class="grid-actions">' +
        '<button onclick="toggleGridActions(event,this)" class="grid-actions-btn" type="button">' +
        '<i data-lucide="settings" class="w-3.5 h-3.5"></i>' +
        '<span>\u0130\u015flemler</span>' +
        '<i data-lucide="chevron-down" class="w-3 h-3"></i>' +
        '</button>' +
        '<template class="grid-actions-tpl">' + menu + '</template>' +
        '</div>';
}

function toggleGridActions(event, btn) {
    event.stopPropagation();
    event.preventDefault();
    var portal = document.getElementById('grid-actions-portal');
    var wasOpen = portal && portal._triggerBtn === btn;
    if (portal) portal.remove();
    if (wasOpen) return;

    var tpl = btn.parentElement.querySelector('.grid-actions-tpl');
    if (!tpl) return;

    var menu = document.createElement('div');
    menu.id = 'grid-actions-portal';
    menu.className = 'grid-actions-portal';
    menu.innerHTML = tpl.innerHTML;
    menu._triggerBtn = btn;

    var isCompact = window.matchMedia('(max-width: 640px)').matches;
    if (isCompact) {
        menu.classList.add('grid-actions-portal--sheet');
        menu.style.cssText = 'position:fixed;left:12px;right:12px;bottom:max(12px, env(safe-area-inset-bottom));top:auto;z-index:9999;';
    } else {
        var rect = btn.getBoundingClientRect();
        var spaceBelow = window.innerHeight - rect.bottom;
        var top = spaceBelow > 220 ? (rect.bottom + 4) : Math.max(8, rect.top - 8);
        var right = window.innerWidth - rect.right;
        menu.style.cssText = 'position:fixed;top:' + top + 'px;right:' + right + 'px;z-index:9999;';
        if (spaceBelow <= 220) menu.style.transform = 'translateY(-100%)';
    }

    document.body.appendChild(menu);
    if (window.lucide) lucide.createIcons({nodes: [menu]});
    if (window.htmx) htmx.process(menu);
}

document.addEventListener('click', function (e) {
    var portal = document.getElementById('grid-actions-portal');
    if (portal && !portal.contains(e.target) && !e.target.closest('.grid-actions-btn')) {
        portal.remove();
    }
});

document.addEventListener('scroll', function () {
    var portal = document.getElementById('grid-actions-portal');
    if (portal) portal.remove();
}, true);

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

window.erpGridDefaults = erpGridDefaults;
window.erpGridAjaxUrl = erpGridAjaxUrl;
window.erpGridAjaxResponse = erpGridAjaxResponse;
window.attachTabulatorPagingAnimation = attachTabulatorPagingAnimation;
window.camelizeTabulatorRows = camelizeTabulatorRows;
window.applyTabulatorTotal = applyTabulatorTotal;
window.gridText = gridText;
window.gridNumber = gridNumber;
window.gridMoney = gridMoney;
window.gridArea = gridArea;
window.erpStatusBadge = erpStatusBadge;
window.erpResponsiveCollapseColumn = erpResponsiveCollapseColumn;
window.gridActionsHtml = gridActionsHtml;
window.bindGridSearch = bindGridSearch;
window.buildExportFilename = buildExportFilename;
window.downloadTable = downloadTable;
window.downloadTableCsv = downloadTableCsv;

