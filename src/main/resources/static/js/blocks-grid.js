// Tabulator 6 Data Grid for Quarry & Block Management
let blocksTable;

function initBlocksGrid() {
    const tableElement = document.getElementById("blocks-table");
    if (!tableElement) return;

    blocksTable = new Tabulator("#blocks-table", {
        layout: "fitColumns",
        responsiveLayout: "collapse",
        pagination: true,
        paginationMode: "remote",
        paginationSize: 10,
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
        placeholder: "Blok kaydı bulunamadı.",
        columns: [
            {
                title: "Blok Kodu",
                field: "blockCode",
                width: 170,
                formatter: function(cell) {
                    const val = cell.getValue();
                    return `<div class="font-mono font-bold text-amber-900">${val}</div>`;
                }
            },
            { title: "Ocak", field: "quarryName", minWidth: 150 },
            { title: "Taş Cinsi", field: "stoneType", minWidth: 130 },
            {
                title: "Ebatlar (En x Boy x Yük.)",
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    return `${row.widthCm}x${row.lengthCm}x${row.heightCm} cm (${row.volumeM3} m³)`;
                }
            },
            {
                title: "Kantar (Fiili / Teorik)",
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
                width: 140,
                formatter: function(cell) {
                    return `<strong>${Number(cell.getValue()).toLocaleString('tr-TR')} TL</strong>`;
                }
            },
            {
                title: "İşlem",
                width: 150,
                headerSort: false,
                formatter: function(cell) {
                    const row = cell.getRow().getData();
                    let html = `<div class="flex items-center gap-1.5">`;
                    if (row.status === 'QUARRY') {
                        html += `<button onclick="transferBlock(${row.id})" class="px-2 py-1 bg-emerald-700 hover:bg-emerald-600 text-white rounded text-xs font-medium transition">Fabrikaya Sevk</button>`;
                    }
                    html += `<a href="/genealogy?code=${row.blockCode}" class="p-1 text-slate-600 hover:text-amber-700" title="Soy Ağacı"><i data-lucide="git-branch" class="w-4 h-4"></i></a>`;
                    html += `</div>`;
                    return html;
                }
            }
        ]
    });

    blocksTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });

    const searchInput = document.getElementById("search-input");
    if (searchInput) {
        let timer;
        searchInput.addEventListener("input", () => {
            clearTimeout(timer);
            timer = setTimeout(() => blocksTable.replaceData(), 300);
        });
    }
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
