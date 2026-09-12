// Tabulator 6 Data Grid for Admin User Management
let usersTable;

function initUsersGrid() {
    const tableElement = document.getElementById("users-table");
    if (!tableElement) return;

    usersTable = new Tabulator("#users-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [5, 10, 25, 50],
        ajaxURL: "/admin/users/api/data",
        ajaxConfig: {
            method: "GET",
            headers: {
                "Accept": "application/json",
            },
        },
        ajaxURLGenerator: function(url, config, params) {
            const searchVal = document.getElementById("search-input")?.value || "";
            const filters = currentUserFilters();
            let sorterField = "";
            let sorterDir = "";
            if (params.sorters && params.sorters.length > 0) {
                sorterField = params.sorters[0].field;
                sorterDir = params.sorters[0].dir;
            }
            let query = `${url}?page=${params.page}&size=${params.size}&search=${encodeURIComponent(searchVal)}&sortField=${sorterField}&sortDir=${sorterDir}`;
            if (filters.role) {
                query += `&role=${encodeURIComponent(filters.role)}`;
            }
            if (filters.enabled !== "") {
                query += `&enabled=${encodeURIComponent(filters.enabled)}`;
            }
            return query;
        },
        ajaxResponse: function(url, params, response) {
            camelizeTabulatorRows(response);
            return applyTabulatorTotal("users-table", response);
        },
        placeholder: "Kullanıcı kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            { title: "ID", field: "id", width: 70, sorter: "number" },
            {
                title: "Kullanıcı",
                field: "username",
                responsive: 0,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `<div>
                        <span class="font-semibold text-slate-800">${row.fullName || row.username}</span>
                        <div class="text-xs text-slate-500">@${row.username}</div>
                    </div>`;
                }
            },
            { title: "E-Posta", field: "email", minWidth: 200 },
            {
                title: "Roller",
                field: "roles",
                formatter: function(cell) {
                    const roles = cell.getValue();
                    if (!roles || roles.length === 0) return `<span class="text-xs text-slate-400">Rol Yok</span>`;
                    return roles.map(r => {
                        const clean = r.replace("ROLE_", "");
                        const isAdm = clean === "ADMIN";
                        return `<span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${isAdm ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'} mr-1">${clean}</span>`;
                    }).join("");
                }
            },
            {
                title: "Durum",
                field: "enabled",
                width: 110,
                formatter: function(cell) {
                    const enabled = cell.getValue();
                    const id = cell.getRow().getData().id;
                    return `<button onclick="toggleUserStatus(${id})" class="px-2.5 py-1 text-xs font-medium rounded-full cursor-pointer transition ${enabled ? 'bg-emerald-100 text-emerald-800 hover:bg-emerald-200' : 'bg-rose-100 text-rose-800 hover:bg-rose-200'}">
                        ${enabled ? '● Aktif' : '○ Askıda'}
                    </button>`;
                }
            },
            {
                title: "Kayıt Tarihi",
                field: "createdAt",
                width: 140,
                formatter: function(cell) {
                    const val = cell.getValue();
                    if (!val) return "-";
                    return new Date(val).toLocaleDateString('tr-TR');
                }
            },
            {
                title: "İşlemler",
                width: 130,
                headerSort: false,
                responsive: 0,
                formatter: function(cell) {
                    const id = cell.getRow().getData().id;
                    return gridActionsHtml([
                        { icon: 'edit-3', label: 'Düzenle', href: '/admin/users/' + id + '/edit' },
                        { icon: 'key', label: 'Şifre Sıfırla', href: '/admin/users/' + id + '/reset-password' },
                        { divider: true },
                        { icon: 'trash-2', label: 'Sil', onclick: 'deleteUser(' + id + ')', danger: true }
                    ]);
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(usersTable);
    bindGridSearch(usersTable, "search-input");
    usersTable.on("renderComplete", function() {
        if (window.lucide) window.lucide.createIcons();
        if (window.htmx && usersTable.element) {
            window.htmx.process(usersTable.element);
        }
    });
}

function currentUserFilters() {
    const root = document.querySelector("[data-users-filters]");
    if (root && window.Alpine && typeof Alpine.$data === "function") {
        const data = Alpine.$data(root);
        return {
            role: data.role || "",
            enabled: data.enabled === undefined || data.enabled === null ? "" : String(data.enabled)
        };
    }
    return {
        role: document.getElementById("filter-role")?.value || "",
        enabled: document.getElementById("filter-enabled")?.value || ""
    };
}

function reloadUsersGrid() {
    if (usersTable) {
        usersTable.replaceData();
    }
}

function toggleUserStatus(id) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`/admin/users/${id}/toggle-status`, {
        method: 'POST',
        headers: headers
    }).then(res => {
        if (res.ok) reloadUsersGrid();
    });
}

function deleteUser(id) {
    if (!confirm("Bu kullanıcıyı silmek (arşivlemek) istediğinizden emin misiniz?")) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`/admin/users/${id}/delete`, {
        method: 'POST',
        headers: headers
    }).then(res => {
        if (res.ok) reloadUsersGrid();
    });
}

document.addEventListener("DOMContentLoaded", initUsersGrid);
