// Tabulator 6 Data Grid for Quarry & Block Management
let blocksTable;

function initBlocksGrid() {
    const tableElement = document.getElementById("blocks-table");
    if (!tableElement) return;

    blocksTable = new Tabulator("#blocks-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [5, 10, 25, 50],
        ajaxURL: "/blocks/api/data",
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
            return applyTabulatorTotal("blocks-table", response);
        },
        placeholder: "Blok kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            {
                title: "Blok Kodu",
                field: "blockCode",
                minWidth: 160,
                width: 170,
                responsive: 0,
                formatter: function(cell) {
                    const val = cell.getValue();
                    return `<div class="font-mono font-bold text-amber-900">${val}</div>`;
                }
            },
            { title: "Ocak", field: "quarryName", minWidth: 140 },
            { title: "Taş Cinsi", field: "stoneType", minWidth: 120 },
            {
                title: "Ebatlar (En x Boy x Yük.)",
                minWidth: 190,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `${row.widthCm}x${row.lengthCm}x${row.heightCm} cm (${row.volumeM3} m³)`;
                }
            },
            {
                title: "Kantar (Fiili / Teorik)",
                minWidth: 160,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    const dev = row.weightDeviationPct;
                    const isNeg = dev < 0;
                    return `<div>
                        <strong>${Number(row.actualWeightKg).toLocaleString('tr-TR')} kg</strong>
                        <span class="text-xs ${isNeg ? 'text-blue-600' : 'text-amber-600'}">(${dev}%)</span>
                    </div>`;
                }
            },
            {
                title: "Kalite",
                field: "qualityGrade",
                minWidth: 80,
                width: 90,
                formatter: function(cell) {
                    const val = cell.getValue();
                    const colors = {
                        'EXTRA': 'bg-purple-100 text-purple-800',
                        'A': 'bg-emerald-100 text-emerald-800',
                        'B': 'bg-blue-100 text-blue-800',
                        'C': 'bg-amber-100 text-amber-800',
                        'MOLOZ': 'bg-rose-100 text-rose-800'
                    };
                    return `<span class="px-2 py-0.5 rounded text-xs font-semibold ${colors[val] || 'bg-slate-100'}">${val}</span>`;
                }
            },
            {
                title: "Durum",
                field: "statusLabel",
                minWidth: 140,
                width: 150,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    const st = row.status;
                    let badge = 'bg-slate-100 text-slate-700';
                    if (st === 'QUARRY') badge = 'bg-amber-100 text-amber-800';
                    if (st === 'FACTORY_STOCK') badge = 'bg-emerald-100 text-emerald-800';
                    if (st === 'SAWING') badge = 'bg-blue-100 text-blue-800';
                    return `<span class="px-2.5 py-1 rounded-full text-xs font-medium ${badge}">● ${cell.getValue()}</span>`;
                }
            },
            {
                title: "Toplam Maliyet",
                field: "totalCost",
                minWidth: 130,
                width: 140,
                formatter: function(cell) {
                    return `<strong>${Number(cell.getValue()).toLocaleString('tr-TR')} TL</strong>`;
                }
            },
            {
                title: "İşlemler",
                minWidth: 120,
                width: 130,
                headerSort: false,
                responsive: 0,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    const items = [];
                    items.push({ icon: 'git-branch', label: 'Soy Ağacı', href: '/genealogy?code=' + row.blockCode });
                    if (row.status === 'QUARRY') {
                        items.push({ icon: 'truck', label: 'Fabrikaya Sevk', onclick: 'transferBlock(' + row.id + ')' });
                    }
                    return gridActionsHtml(items);
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(blocksTable);
    bindGridSearch(blocksTable, "search-input");
    blocksTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });
}

function reloadBlocksGrid() {
    if (blocksTable) blocksTable.replaceData();
}

function transferBlock(id) {
    const cost = prompt("Fabrikaya iç transfer nakliye bedelini giriniz (TL):", "7200");
    if (!cost) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;

    fetch(`/blocks/${id}/transfer-to-factory`, {
        method: 'POST',
        headers: headers,
        body: `transportCost=${encodeURIComponent(cost)}`
    }).then(res => {
        if (res.ok) reloadBlocksGrid();
    });
}

document.addEventListener("DOMContentLoaded", initBlocksGrid);
