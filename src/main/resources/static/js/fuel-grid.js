// Tabulator 6 Data Grid for Machine Fuel & Operating Hours Tracking
let fuelTable;

function fuelPage() {
    return {
        unitFilter: "",
        period: "",
        machineId: "",
        init() {
            window.fuelPageState = this;
        },
        setUnitFilter(unit) {
            this.unitFilter = unit;
            if (typeof reloadFuelGrid === "function") {
                reloadFuelGrid();
            }
        },
        gridExtraQuery() {
            let q = "";
            if (this.unitFilter) {
                q += "&unit=" + encodeURIComponent(this.unitFilter);
            }
            const periodInput = document.getElementById("fuel-period-filter");
            if (periodInput && periodInput.value) {
                q += "&period=" + encodeURIComponent(periodInput.value);
            }
            const machineSelect = document.getElementById("fuel-machine-filter");
            if (machineSelect && machineSelect.value) {
                q += "&machineId=" + encodeURIComponent(machineSelect.value);
            }
            return q;
        }
    };
}

function initFuelGrid() {
    const tableElement = document.getElementById("fuel-table");
    if (!tableElement) return;

    fuelTable = new Tabulator("#fuel-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [10, 25, 50, 100],
        columnCalcs: "bottom",
        ajaxURL: "/machines/fuel/api/data",
        ajaxConfig: {
            method: "GET",
            headers: { "Accept": "application/json" }
        },
        ajaxURLGenerator: function (url, config, params) {
            return erpGridAjaxUrl(url, params, "search-input", function () {
                return window.fuelPageState ? window.fuelPageState.gridExtraQuery() : "";
            });
        },
        ajaxResponse: function (url, params, response) {
            return erpGridAjaxResponse("fuel-table", response);
        },
        placeholder: "Bu kritere uygun mazot dolum kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            erpIndexColumn(),
            {
                title: "Tarih",
                field: "entryDate",
                minWidth: 110,
                width: 120,
                responsive: 0,
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (!val) return '<span class="text-slate-400">—</span>';
                    const parts = String(val).split("-");
                    if (parts.length === 3) {
                        return `<span class="font-mono text-xs text-slate-700">${parts[2]}.${parts[1]}.${parts[0]}</span>`;
                    }
                    return `<span class="font-mono text-xs text-slate-700">${gridText(val)}</span>`;
                }
            },
            {
                title: "Makine",
                field: "machineName",
                minWidth: 160,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const code = row.machineCode ? `<span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 ml-1.5">${gridText(row.machineCode)}</span>` : "";
                    return `<span class="font-semibold text-slate-900">${gridText(cell.getValue())}</span>${code}`;
                }
            },
            {
                title: "Birim",
                field: "businessUnitLabel",
                minWidth: 100,
                width: 110,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const isQuarry = row.businessUnit === "QUARRY";
                    const badgeClass = isQuarry
                        ? "bg-amber-50 text-amber-800 border-amber-200"
                        : "bg-indigo-50 text-indigo-800 border-indigo-200";
                    return `<span class="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] font-semibold border ${badgeClass}">${gridText(cell.getValue() || row.businessUnit)}</span>`;
                }
            },
            {
                title: "Litre (L)",
                field: "litres",
                minWidth: 110,
                width: 120,
                hozAlign: "right",
                headerHozAlign: "right",
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (val == null) return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono font-bold text-slate-900">${Number(val).toLocaleString("tr-TR", { minimumFractionDigits: 1, maximumFractionDigits: 2 })} L</span>`;
                },
                bottomCalc: "sum",
                bottomCalcFormatter: function (cell) {
                    const val = cell.getValue();
                    return `<span class="font-mono font-bold text-slate-900">${Number(val || 0).toLocaleString("tr-TR", { minimumFractionDigits: 1, maximumFractionDigits: 2 })} L</span>`;
                }
            },
            {
                title: "Litre Fiyatı",
                field: "pricePerLitre",
                minWidth: 110,
                width: 120,
                hozAlign: "right",
                headerHozAlign: "right",
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (val == null) return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono text-slate-700">${Number(val).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₺</span>`;
                }
            },
            {
                title: "Toplam Tutar",
                field: "totalAmount",
                minWidth: 130,
                width: 140,
                hozAlign: "right",
                headerHozAlign: "right",
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (val == null) return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono font-bold text-amber-950">${Number(val).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₺</span>`;
                },
                bottomCalc: "sum",
                bottomCalcFormatter: function (cell) {
                    const val = cell.getValue();
                    return `<span class="font-mono font-bold text-amber-950">${Number(val || 0).toLocaleString("tr-TR", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ₺</span>`;
                }
            },
            {
                title: "Çalışma Saati / Km",
                field: "workingHoursOrKm",
                minWidth: 140,
                width: 150,
                hozAlign: "right",
                headerHozAlign: "right",
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (val == null) return '<span class="text-slate-400 font-mono text-xs">—</span>';
                    return `<span class="font-mono text-xs font-semibold text-slate-700">${Number(val).toLocaleString("tr-TR", { minimumFractionDigits: 1, maximumFractionDigits: 1 })} sa/km</span>`;
                }
            },
            {
                title: "Fiş No",
                field: "receiptNo",
                minWidth: 100,
                width: 110,
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (!val) return '<span class="text-slate-400 font-mono text-xs">—</span>';
                    return `<span class="font-mono text-xs text-slate-600">${gridText(val)}</span>`;
                }
            },
            {
                title: "Teslim Alan / Veren",
                field: "receivedBy",
                minWidth: 140,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const rec = row.receivedBy ? gridText(row.receivedBy) : "";
                    const iss = row.issuedBy ? gridText(row.issuedBy) : "";
                    if (!rec && !iss) return '<span class="text-slate-400 text-xs">—</span>';
                    if (rec && iss) return `<span class="text-xs text-slate-700">${rec} <span class="text-slate-400">/</span> ${iss}</span>`;
                    return `<span class="text-xs text-slate-700">${rec || iss}</span>`;
                }
            },
            {
                title: "İşlem",
                width: 90,
                headerSort: false,
                hozAlign: "center",
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    return `<button type="button" onclick="confirmFuelDelete(${row.id}, '${gridText(row.machineName)}', '${row.litres}')"
                            class="p-1.5 text-rose-600 hover:text-rose-800 hover:bg-rose-50 rounded-lg transition" title="Sil">
                            <i data-lucide="trash-2" class="w-4 h-4"></i>
                        </button>`;
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(fuelTable);
    bindGridSearch(fuelTable, "search-input");
    fuelTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });
}

function reloadFuelGrid() {
    if (fuelTable) {
        fuelTable.setData();
    }
}

function confirmFuelDelete(id, machineName, litres) {
    if (window.fuelAlpineComponent) {
        window.fuelAlpineComponent.openDeleteModal(id, machineName, litres);
    } else if (confirm(`Bu mazot dolum kaydını (${machineName} - ${litres} L) silmek istediğinize emin misiniz?`)) {
        submitFuelDelete(id);
    }
}

function submitFuelDelete(id) {
    const form = document.createElement("form");
    form.method = "POST";
    form.action = `/machines/fuel/${id}/delete`;
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
    const csrfParam = document.querySelector('meta[name="_csrf_parameter_name"]')?.content || "_csrf";
    if (csrfToken) {
        const input = document.createElement("input");
        input.type = "hidden";
        input.name = csrfParam;
        input.value = csrfToken;
        form.appendChild(input);
    }
    document.body.appendChild(form);
    form.submit();
}

document.addEventListener("DOMContentLoaded", () => {
    initFuelGrid();
});

window.initFuelGrid = initFuelGrid;
window.reloadFuelGrid = reloadFuelGrid;
window.confirmFuelDelete = confirmFuelDelete;
window.fuelPage = fuelPage;
