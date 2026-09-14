// Tabulator 6 Data Grid for Factory Production Orders
let productionTable;

function initProductionGrid() {
    const tableElement = document.getElementById("production-table");
    if (!tableElement) return;

    productionTable = new Tabulator("#production-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [5, 10, 25, 50],
        ajaxURL: "/production/api/orders",
        ajaxConfig: {
            method: "GET",
            headers: {"Accept": "application/json"},
        },
        ajaxURLGenerator: function (url, config, params) {
            return erpGridAjaxUrl(url, params, "search-input");
        },
        ajaxResponse: function (url, params, response) {
            return erpGridAjaxResponse("production-table", response);
        },
        placeholder: "Üretim emri kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            {
                title: "İş Emri No",
                field: "orderNo",
                width: 170,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<a href="/production/orders/${row.id}" class="font-mono font-bold text-amber-900 hover:text-amber-700 underline">${gridText(cell.getValue())}</a>`;
                }
            },
            {
                title: "Kaynak Blok",
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<div>
                        <strong class="text-slate-800">${gridText(row.blockCode)}</strong>
                        <div class="text-xs text-slate-500">${gridText(row.stoneType)}</div>
                    </div>`;
                }
            },
            {title: "Makine / Hat", field: "machineName", minWidth: 150},
            {
                title: "Süre / Tüketim",
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<div class="text-xs">
                        <div>Süre: <strong>${gridNumber(row.durationHours)} sa</strong></div>
                        <div class="text-slate-500">Enerji: ${gridNumber(row.electricityKwh)} kWh</div>
                    </div>`;
                }
            },
            {
                title: "Çıkan Plakalar",
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<div>
                        <span class="font-bold text-emerald-800">${gridText(row.slabCount)} Plaka</span>
                        <div class="text-xs text-slate-500">${gridArea(row.totalSlabAreaM2)}</div>
                    </div>`;
                }
            },
            {title: "Operatör", field: "operatorName", width: 140},
            {
                title: "Durum",
                field: "status",
                width: 130,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return erpStatusBadge(row.status, row.statusLabel);
                }
            },
            {
                title: "İşlemler",
                width: 120,
                headerSort: false,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return gridActionsHtml([
                        {icon: 'eye', label: 'Detay', href: '/production/orders/' + row.id},
                        {icon: 'edit-3', label: 'Düzenle', href: '/production/orders/' + row.id + '/edit'},
                        {icon: 'git-branch', label: 'Soy Ağacı', href: '/genealogy?code=' + row.orderNo}
                    ]);
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(productionTable);
    bindGridSearch(productionTable, "search-input");
    productionTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });
}

function reloadProductionGrid() {
    if (productionTable) productionTable.replaceData();
}

document.addEventListener("DOMContentLoaded", initProductionGrid);
