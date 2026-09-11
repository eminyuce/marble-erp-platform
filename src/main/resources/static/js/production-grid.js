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
        paginationSize: 10,
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
            if (response && Array.isArray(response.data)) {
                response.data.forEach(item => {
                    Object.keys(item).forEach(key => {
                        const camel = key.replace(/_([a-z0-9])/g, (_, l) => l.toUpperCase());
                        if (!(camel in item)) item[camel] = item[key];
                    });
                });
            }
            return response;
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
                width: 100,
                headerSort: false,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `<a href="/genealogy?code=${row.orderNo}" class="p-1.5 text-slate-600 hover:text-amber-700 inline-block" title="Soy Ağacı"><i data-lucide="git-branch" class="w-4 h-4"></i></a>`;
                }
            }
        ]
    });

    productionTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });

    const searchInput = document.getElementById("search-input");
    if (searchInput) {
        let timer;
        searchInput.addEventListener("input", () => {
            clearTimeout(timer);
            timer = setTimeout(() => productionTable.replaceData(), 300);
        });
    }
}

function reloadProductionGrid() {
    if (productionTable) productionTable.replaceData();
}

document.addEventListener("DOMContentLoaded", initProductionGrid);
