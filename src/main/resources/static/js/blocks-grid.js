// Tabulator 6 Data Grid for quarry block production
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
            headers: {"Accept": "application/json"},
        },
        ajaxURLGenerator: function (url, config, params) {
            return erpGridAjaxUrl(url, params, "search-input");
        },
        ajaxResponse: function (url, params, response) {
            return erpGridAjaxResponse("blocks-table", response);
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
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<a href="/blocks/${row.id}" class="font-mono font-bold text-amber-900 hover:text-amber-700 underline">${gridText(cell.getValue())}</a>`;
                }
            },
            {title: "Ocak", field: "quarryName", minWidth: 140},
            {title: "Saha", field: "locationName", minWidth: 140},
            {title: "Taş Cinsi", field: "stoneType", minWidth: 120},
            {
                title: "Ebatlar (En x Boy x Yük.)",
                minWidth: 190,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `${gridText(row.widthCm)}x${gridText(row.lengthCm)}x${gridText(row.heightCm)} cm (${gridNumber(row.volumeM3)} m³)`;
                }
            },
            {
                title: "Tonaj (yaklaşık / fiili)",
                minWidth: 170,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const warn = row.weightDeviationWarning;
                    return `<div>
                        <strong>${gridNumber(row.approximateTonnage)} / ${gridNumber(row.actualTonnage)} ton</strong>
                        <div class="text-xs ${warn ? 'text-rose-700 font-semibold' : 'text-slate-500'}">
                            sapma ${gridNumber(row.weightDeviationPct)}%${warn ? ' · %5 uyarısı' : ''}
                        </div>
                    </div>`;
                }
            },
            {
                title: "Kalite",
                field: "qualityGradeLabel",
                minWidth: 90,
                width: 110,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const val = row.qualityGrade;
                    const colors = {
                        'EXTRA': 'bg-purple-100 text-purple-800',
                        'A': 'bg-emerald-100 text-emerald-800',
                        'B': 'bg-blue-100 text-blue-800',
                        'C': 'bg-amber-100 text-amber-800',
                        'MOLOZ': 'bg-rose-100 text-rose-800'
                    };
                    return `<span class="px-2 py-0.5 rounded text-xs font-semibold ${colors[val] || 'bg-slate-100'}">${gridText(cell.getValue())}</span>`;
                }
            },
            {
                title: "Durum",
                field: "statusLabel",
                minWidth: 140,
                width: 160,
                formatter: function (cell) {
                    return `<span class="px-2.5 py-1 rounded-full text-xs font-medium bg-slate-100 text-slate-700">● ${gridText(cell.getValue())}</span>`;
                }
            },
            {
                title: "Toplam Maliyet",
                field: "totalCost",
                minWidth: 130,
                width: 140,
                formatter: function (cell) {
                    return `<strong>${gridMoney(cell.getValue())}</strong>`;
                }
            },
            {
                title: "İşlemler",
                minWidth: 120,
                width: 130,
                headerSort: false,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const items = [];
                    items.push({icon: 'eye', label: 'Detay', href: '/blocks/' + row.id});
                    items.push({icon: 'git-branch', label: 'Soy Ağacı', href: '/genealogy?code=' + row.blockCode});
                    items.push({icon: 'edit-3', label: 'Düzenle', href: '/blocks/' + row.id + '/edit'});
                    const atQuarry = row.canonicalStatus === 'PRODUCED' || row.canonicalStatus === 'MARKED'
                        || row.status === 'QUARRY' || row.status === 'PRODUCED' || row.status === 'MARKED';
                    if (atQuarry) {
                        items.push({icon: 'truck', label: 'Fabrikaya Sevk', onclick: 'transferBlock(' + row.id + ')'});
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
    const headers = {'Content-Type': 'application/x-www-form-urlencoded'};
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
