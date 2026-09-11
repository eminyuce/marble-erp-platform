// Tabulator 6 Data Grid for Admin User Management
let usersTable;

function initUsersGrid() {
    const tableElement = document.getElementById("users-table");
    if (!tableElement) return;

    usersTable = new Tabulator("#users-table", {
        layout: "fitColumns",
        responsiveLayout: "collapse",
        pagination: true,
        paginationMode: "remote",
        paginationSize: 10,
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
            let sorterField = "";
            let sorterDir = "";
            if (params.sorters && params.sorters.length > 0) {
                sorterField = params.sorters[0].field;
                sorterDir = params.sorters[0].dir;
            }
            return `${url}?page=${params.page}&size=${params.size}&search=${encodeURIComponent(searchVal)}&sortField=${sorterField}&sortDir=${sorterDir}`;
        },
        placeholder: "Kullanıcı kaydı bulunamadı.",
        columns: [
            { title: "ID", field: "id", width: 70, sorter: "number" },
            {
                title: "Kullanıcı",
                field: "username",
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
                width: 180,
                headerSort: false,
                formatter: function(cell) {
                    const id = cell.getRow().getData().id;
                    return `<div class="flex items-center gap-1">
                        <button hx-get="/admin/users/${id}/edit" hx-target="#modal-container" class="p-1.5 text-slate-600 hover:text-blue-600 hover:bg-slate-100 rounded transition" title="Düzenle">
                            <i data-lucide="edit-3" class="w-4 h-4"></i>
                        </button>
                        <button hx-get="/admin/users/${id}/reset-password" hx-target="#modal-container" class="p-1.5 text-slate-600 hover:text-amber-600 hover:bg-slate-100 rounded transition" title="Şifre Sıfırla">
                            <i data-lucide="key" class="w-4 h-4"></i>
                        </button>
                        <button onclick="deleteUser(${id})" class="p-1.5 text-slate-600 hover:text-rose-600 hover:bg-slate-100 rounded transition" title="Sil">
                            <i data-lucide="trash-2" class="w-4 h-4"></i>
                        </button>
                    </div>`;
                }
            }
        ]
    });

    usersTable.on("renderComplete", function() {
        if (window.lucide) window.lucide.createIcons();
    });

    const searchInput = document.getElementById("search-input");
    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener("input", () => {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(() => {
                usersTable.replaceData();
            }, 300);
        });
    }
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
