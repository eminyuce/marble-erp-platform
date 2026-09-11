// Tabulator 6 Data Grid for Factory Production Orders
let productionTable;

function initProductionGrid() {
    const tableElement = document.getElementById("production-table");
    if (!tableElement) return;

    productionTable = new Tabulator("#production-table", {
        layout: "fitColumns",
        responsiveLayout: "collapse",
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [5, 10, 25, 50],
        ajaxURL: "/production/api/orders",
        ajaxConfig: {
            method: "GET",
            headers: { "Accept": "application/json" },
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
        ajaxResponse: function(url, params, response) {
            camelizeTabulatorRows(response);
            return applyTabulatorTotal("production-table", response);
        },
        placeholder: "Üretim emri kaydı bulunamadı.",
        columns: [
            {
                title: "İş Emri No",
                field: "orderNo",
                width: 170,
                formatter: function(cell) {
                    return `<span class="font-mono font-bold text-amber-900">${cell.getValue()}</span>`;
                }
            },
            {
                title: "Kaynak Blok",
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `<div>
                        <strong class="text-slate-800">${row.blockCode}</strong>
                        <div class="text-xs text-slate-500">${row.stoneType}</div>
                    </div>`;
                }
            },
            { title: "Makine / Hat", field: "machineName", minWidth: 150 },
            {
                title: "Süre / Tüketim",
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `<div class="text-xs">
                        <div>Süre: <strong>${row.durationHours} sa</strong></div>
                        <div class="text-slate-500">Enerji: ${row.electricityKwh} kWh</div>
                    </div>`;
                }
            },
            {
                title: "Çıkan Plakalar",
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `<div>
                        <span class="font-bold text-emerald-800">${row.slabCount} Plaka</span>
                        <div class="text-xs text-slate-500">${Number(row.totalSlabAreaM2).toFixed(2)} m²</div>
                    </div>`;
                }
            },
            { title: "Operatör", field: "operatorName", width: 140 },
            {
                title: "Durum",
                field: "status",
                width: 110,
                formatter: function(cell) {
                    return `<span class="px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">● ${cell.getValue()}</span>`;
                }
            },
            {
                title: "İşlemler",
                width: 120,
                headerSort: false,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return gridActionsHtml([
                        { icon: 'git-branch', label: 'Soy Ağacı', href: '/genealogy?code=' + row.orderNo }
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
