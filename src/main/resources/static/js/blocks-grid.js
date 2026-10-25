// Tabulator 6 Data Grid for quarry block production
let blocksTable;
let pendingDeleteBlockId = null;

function quarryPage() {
    return {
        stockFilter: "IN_STOCK",
        yardFilter: "",
        statusFilter: "",
        init() {
            window.quarryPageState = this;
        },
        setStockFilter(filter) {
            this.stockFilter = filter;
            this.yardFilter = "";
            this.statusFilter = "";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        setYardFilter(filter) {
            this.yardFilter = filter;
            this.statusFilter = "";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        setStatusFilter(status) {
            this.statusFilter = status;
            this.yardFilter = "";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        gridExtraQuery() {
            let q = "";
            if (this.stockFilter) {
                q += "&stockFilter=" + encodeURIComponent(this.stockFilter);
            }
            if (this.yardFilter) {
                q += "&locationType=" + encodeURIComponent(this.yardFilter);
            }
            if (this.statusFilter) {
                q += "&status=" + encodeURIComponent(this.statusFilter);
            }
            return q;
        }
    };
}

function initBlocksGrid() {
    const tableElement = document.getElementById("blocks-table");
    if (!tableElement) return;

    blocksTable = new Tabulator("#blocks-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [5, 10, 25, 50],
        columnCalcs: "bottom",
        ajaxURL: "/blocks/api/data",
        ajaxConfig: {
            method: "GET",
            headers: {"Accept": "application/json"},
        },
        ajaxURLGenerator: function (url, config, params) {
            return erpGridAjaxUrl(url, params, "search-input", function () {
                return window.quarryPageState ? window.quarryPageState.gridExtraQuery() : "";
            });
        },
        ajaxResponse: function (url, params, response) {
            return erpGridAjaxResponse("blocks-table", response);
        },
        placeholder: "Blok kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            erpIndexColumn(),
            {
                title: "Blok Kodu",
                field: "blockCode",
                minWidth: 160,
                width: 170,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const code = String(cell.getValue() || "").toLocaleUpperCase("en-US");
                    return `<a href="/blocks/${row.id}" class="font-mono font-bold text-amber-900 hover:text-amber-700 underline">${gridText(code)}</a>`;
                }
            },
            {
                title: "Oluşturma Tarihi",
                field: "extractionDate",
                minWidth: 125,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const d = cell.getValue() || row.createdAt || row.created_at;
                    return `<span class="text-xs text-slate-700 font-mono font-medium">${gridDate(d)}</span>`;
                }
            },
            {
                title: "Saha",
                field: "locationName",
                minWidth: 130,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const name = gridText(cell.getValue());
                    if (!name || name === "—") return '<span class="text-slate-400">—</span>';
                    const badge = row.locationType === "PRODUCTION_YARD"
                        ? "bg-amber-100 text-amber-800"
                        : row.locationType === "DISPATCH_YARD"
                            ? "bg-sky-100 text-sky-800"
                            : "bg-slate-100 text-slate-700";
                    return `<span class="px-2 py-0.5 rounded text-xs font-semibold ${badge}">${name}</span>`;
                }
            },
            {
                title: "Müşteri",
                field: "soldCustomerName",
                minWidth: 150,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const sold = row.canonicalStatus === "SOLD" || row.status === "SOLD";
                    if (sold) {
                        const name = gridText(cell.getValue());
                        return name && name !== "—"
                            ? `<span class="font-semibold text-purple-800">${name}</span>`
                            : '<span class="text-slate-400">Belirtilmedi</span>';
                    }
                    if (row.assignedCustomerName) {
                        const price = (row.salePrice && row.salePrice > 0) ? ` (${gridMoney(row.salePrice)})` : '';
                        return `<span class="px-2 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300">Rezerve: ${gridText(row.assignedCustomerName)}${price}</span>`;
                    }
                    return '<span class="text-slate-400">—</span>';
                }
            },
            {title: "Ocak", field: "quarryName", minWidth: 120},
            {title: "Ocak Bölgesi", field: "quarrySection", minWidth: 110, formatter: (cell) => gridText(cell.getValue())},
            {title: "Taş Cinsi", field: "stoneType", minWidth: 100, formatter: (cell) => gridText(cell.getValue())},
            {title: "Seleksiyon", field: "colorTone", minWidth: 100, formatter: (cell) => gridText(cell.getValue())},
            {
                title: "Tonaj",
                field: "approximateTonnage",
                minWidth: 110,
                bottomCalc: blocksPageCalc("totalTonnage"),
                bottomCalcFormatter: function (cell) {
                    return `<strong class="erp-grid-calc-value">${formatBlocksMetric(cell.getValue(), "t")}</strong>`;
                },
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const warn = row.weightDeviationWarning;
                    const ton = row.actualTonnage > 0
                        ? `${gridNumber(row.approximateTonnage)} / ${gridNumber(row.actualTonnage)} t`
                        : `${gridNumber(row.approximateTonnage)} t`;
                    return `<div>
                        <strong>${ton}</strong>
                        ${warn ? '<div class="text-xs text-rose-700 font-semibold">%5 sapma uyarısı</div>' : ""}
                    </div>`;
                }
            },
            {
                title: "m²",
                field: "surfaceAreaM2",
                minWidth: 90,
                bottomCalc: blocksPageCalc("totalSurfaceM2"),
                bottomCalcFormatter: function (cell) {
                    return `<strong class="erp-grid-calc-value">${formatBlocksMetric(cell.getValue(), "m²")}</strong>`;
                },
                formatter: (cell) => gridArea(cell.getValue())
            },
            {
                title: "Piyasa Değeri",
                field: "marketValue",
                minWidth: 120,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    if (row.marketValue) {
                        return `<strong class="text-emerald-800">${gridMoney(row.marketValue)}</strong>`;
                    }
                    if (row.unitMarketValuePerTon) {
                        return `<span class="text-xs text-slate-600">${gridMoney(row.unitMarketValuePerTon)}/ton</span>`;
                    }
                    return '<span class="text-slate-400">—</span>';
                }
            },
            {
                title: "Durum",
                field: "statusLabel",
                minWidth: 130,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const status = row.status || row.canonicalStatus;
                    let cls = "bg-slate-100 text-slate-700";
                    if (status === "PRODUCED") cls = "bg-amber-100 text-amber-800";
                    else if (status === "READY_FOR_DISPATCH") cls = "bg-emerald-100 text-emerald-800";
                    else if (status === "MARKED") cls = "bg-amber-100 text-amber-900 border border-amber-300";
                    else if (status === "IN_TRANSIT") cls = "bg-indigo-100 text-indigo-800";
                    else if (status === "AT_FACTORY" || status === "FACTORY_STOCK") cls = "bg-blue-100 text-blue-800";
                    else if (status === "IN_PROCESS" || status === "SAWING") cls = "bg-cyan-100 text-cyan-800";
                    else if (status === "SOLD") cls = "bg-purple-100 text-purple-800";
                    return `<span class="px-2 py-0.5 rounded-full text-xs font-semibold ${cls}">${gridText(cell.getValue())}</span>`;
                }
            },
            {
                title: "Çıkarma Maliyeti",
                field: "calculatedExtractionCost",
                minWidth: 130,
                bottomCalc: blocksPageCalc("totalExtractionCost"),
                bottomCalcFormatter: function (cell) {
                    return `<strong class="erp-grid-calc-value">${gridMoney(cell.getValue())}</strong>`;
                },
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const val = row.calculatedExtractionCost != null ? row.calculatedExtractionCost : row.extractionCost;
                    return `<strong>${gridMoney(val)}</strong>`;
                }
            },
            {
                title: "Toplam Maliyet",
                field: "totalCost",
                minWidth: 120,
                bottomCalc: blocksPageCalc("totalCost"),
                bottomCalcFormatter: function (cell) {
                    return `<strong class="erp-grid-calc-value">${gridMoney(cell.getValue())}</strong>`;
                },
                formatter: function (cell) {
                    return `<strong>${gridMoney(cell.getValue())}</strong>`;
                }
            },
            {
                title: "İşlemler",
                minWidth: 110,
                width: 120,
                headerSort: false,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const items = [];
                    items.push({icon: "eye", label: "Detay ve işlemler", href: "/blocks/" + row.id});
                    items.push({icon: "edit-3", label: "Düzenle", href: "/blocks/" + row.id + "/edit"});
                    const sold = row.canonicalStatus === "SOLD" || row.status === "SOLD";
                    const atQuarry = !sold && (row.canonicalStatus === "PRODUCED" || row.canonicalStatus === "READY_FOR_DISPATCH" || row.canonicalStatus === "MARKED"
                        || row.status === "QUARRY" || row.status === "PRODUCED" || row.status === "READY_FOR_DISPATCH" || row.status === "MARKED");
                    if (atQuarry) {
                        items.push({
                            icon: "scale",
                            label: "Kantar Tartımı Gir",
                            onclick: "openActualTonnageModal(" + row.id + ", '" + (row.blockCode || "") + "', " + (row.approximateTonnage || 0) + ")"
                        });
                        if (row.locationType !== "PRODUCTION_YARD") {
                            items.push({
                                icon: "pickaxe",
                                label: "Üretim Sahasına taşı",
                                href: "/blocks/" + row.id + "/move?targetType=PRODUCTION_YARD"
                            });
                        }
                        if (row.locationType !== "DISPATCH_YARD") {
                            items.push({
                                icon: "warehouse",
                                label: "Stok Sahasına taşı",
                                href: "/blocks/" + row.id + "/move?targetType=DISPATCH_YARD"
                            });
                        }
                        items.push({
                            icon: "bookmark",
                            label: "Markala (Rezerve Et)",
                            onclick: "openMarkBlockModal(" + row.id + ", '" + (row.blockCode || "") + "', " + (row.salePrice || row.marketValue || 0) + ", " + (row.assignedCustomerId || "null") + ")"
                        });
                        if (row.locationType === "DISPATCH_YARD") {
                            items.push({icon: "truck", label: "Fabrikaya sevk", href: "/blocks/" + row.id + "/transfer-to-factory"});
                        }
                        items.push({
                            icon: "receipt",
                            label: "Sat (Fatura Kes)",
                            href: "/blocks/" + row.id + "/sell"
                        });
                    }
                    if (row.canDelete) {
                        items.push({
                            icon: "trash-2",
                            label: "Bloğu Sil",
                            danger: true,
                            onclick: "openDeleteBlock(" + row.id + ", '" + (row.blockCode || "") + "')"
                        });
                    }
                    return gridActionsHtml(items);
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(blocksTable);
    bindGridSearch(blocksTable, "search-input");
    bindBlocksGridIcons(blocksTable);
}

function reloadBlocksGrid() {
    if (blocksTable) {
        blocksTable.setPage(1);
    }
}

function toFiniteNumber(value) {
    const number = Number(value);
    return Number.isFinite(number) ? number : 0;
}

function isDataBlockRow(row) {
    return !!(row && row.id != null);
}

function rowTonnage(row) {
    const actual = toFiniteNumber(row.actualTonnage);
    return actual > 0 ? actual : toFiniteNumber(row.approximateTonnage);
}

function rowExtractionCost(row) {
    return row.calculatedExtractionCost != null
        ? toFiniteNumber(row.calculatedExtractionCost)
        : toFiniteNumber(row.extractionCost);
}

function sumBlockRows(rows) {
    const totals = {
        totalTonnage: 0,
        totalSurfaceM2: 0,
        totalExtractionCost: 0,
        totalCost: 0
    };
    (rows || []).forEach(function (row) {
        if (!isDataBlockRow(row)) {
            return;
        }
        totals.totalTonnage += rowTonnage(row);
        totals.totalSurfaceM2 += toFiniteNumber(row.surfaceAreaM2);
        totals.totalExtractionCost += rowExtractionCost(row);
        totals.totalCost += toFiniteNumber(row.totalCost);
    });
    return totals;
}

function blocksPageCalc(metricKey) {
    return function (values, data) {
        const totals = sumBlockRows(data);
        return toFiniteNumber(totals[metricKey]);
    };
}

function formatBlocksMetric(value, suffix) {
    const formatted = gridNumber(value, function (n) {
        return n.toLocaleString("tr-TR", {minimumFractionDigits: 2, maximumFractionDigits: 2});
    });
    if (formatted === "—") {
        return "—";
    }
    return suffix ? formatted + " " + suffix : formatted;
}

function bindBlocksGridIcons(table) {
    const refresh = function () {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    };
    table.on("dataLoaded", refresh);
    table.on("dataProcessed", refresh);
    table.on("pageLoaded", refresh);
    table.on("renderComplete", refresh);
}

window.quarryPage = quarryPage;
window.initBlocksGrid = initBlocksGrid;
window.reloadBlocksGrid = reloadBlocksGrid;

function csrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = {"Content-Type": "application/x-www-form-urlencoded"};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    return headers;
}

function showBlocksToast(message, ok) {
    const existing = document.querySelector(".erp-toast");
    if (existing) existing.remove();
    const toast = document.createElement("div");
    toast.className = "erp-toast " + (ok ? "erp-toast--ok" : "erp-toast--err");
    toast.setAttribute("role", "alert");
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

function isSuccessfulBackendResponse(body) {
    const code = body && body.serviceStatus && body.serviceStatus.status
        ? body.serviceStatus.status.errorCode
        : undefined;
    return code === undefined || code === "0" || code === 0;
}

function handleBlockActionResponse(res, okMessage) {
    return res.json().then(function (body) {
        if (res.ok && isSuccessfulBackendResponse(body)) {
            showBlocksToast(okMessage, true);
            reloadBlocksGrid();
            return;
        }
        showBlocksToast("İşlem tamamlanamadı. Lütfen tekrar deneyin veya blok detayından deneyin.", false);
    }).catch(function () {
        if (res.ok) {
            showBlocksToast(okMessage, true);
            reloadBlocksGrid();
            return;
        }
        showBlocksToast("İşlem tamamlanamadı. Lütfen tekrar deneyin veya blok detayından deneyin.", false);
    });
}

function openDeleteBlock(id, blockCode) {
    pendingDeleteBlockId = id;
    const dialog = document.getElementById("delete-block-dialog");
    const codeEl = document.getElementById("delete-block-code");
    if (codeEl) codeEl.textContent = blockCode || ("ID: " + id);
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksToast("Silme onay penceresi açılamadı.", false);
}

function deleteBlock(id) {
    fetch(`/blocks/${id}/api/delete`, {
        method: "POST",
        headers: csrfHeaders()
    }).then(res => handleBlockActionResponse(res, "Blok başarıyla silindi."));
}

let pendingTonnageBlockId = null;
function openActualTonnageModal(id, code, approxTon) {
    pendingTonnageBlockId = id;
    const dialog = document.getElementById("tonnage-modal-dialog");
    const codeEl = document.getElementById("tonnage-block-code");
    const approxEl = document.getElementById("tonnage-approx");
    const inputEl = document.getElementById("input-actual-tonnage");
    if (codeEl) codeEl.textContent = code || ("ID: " + id);
    if (approxEl) approxEl.textContent = (approxTon || 0) + " Ton";
    if (inputEl) inputEl.value = "";
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksToast("Tartım penceresi açılamadı.", false);
}

function saveActualTonnage(id, tonnage) {
    const body = new URLSearchParams();
    body.append("actualTonnage", tonnage);
    fetch(`/blocks/${id}/api/actual-tonnage`, {
        method: "POST",
        headers: csrfHeaders(),
        body: body
    }).then(res => handleBlockActionResponse(res, "Kantar tartımı kaydedildi (Tartıldı / Sevke Hazır)."));
}

window.openActualTonnageModal = openActualTonnageModal;
window.openDeleteBlock = openDeleteBlock;

document.addEventListener("DOMContentLoaded", function () {
    initBlocksGrid();

    document.getElementById("confirm-delete-block")?.addEventListener("click", function () {
        const dialog = document.getElementById("delete-block-dialog");
        if (!pendingDeleteBlockId) return;
        deleteBlock(pendingDeleteBlockId);
        if (dialog) dialog.close();
        pendingDeleteBlockId = null;
    });

    document.querySelectorAll("#delete-block-dialog button[value='cancel'], #delete-cancel")
        .forEach(function (btn) {
            btn.addEventListener("click", function () {
                btn.closest("dialog")?.close();
            });
        });

    document.getElementById("confirm-save-tonnage")?.addEventListener("click", function () {
        const dialog = document.getElementById("tonnage-modal-dialog");
        const inputEl = document.getElementById("input-actual-tonnage");
        const val = inputEl ? parseFloat(inputEl.value) : null;
        if (!val || isNaN(val) || val <= 0) {
            showBlocksToast("Lütfen geçerli bir kantar tonajı girin.", false);
            return;
        }
        if (!pendingTonnageBlockId) return;
        saveActualTonnage(pendingTonnageBlockId, val);
        if (dialog) dialog.close();
        pendingTonnageBlockId = null;
    });

    document.querySelectorAll("#tonnage-modal-dialog button[value='cancel'], #tonnage-cancel")
        .forEach(function (btn) {
            btn.addEventListener("click", function () {
                btn.closest("dialog")?.close();
            });
        });

    document.getElementById("confirm-mark-block")?.addEventListener("click", function () {
        const dialog = document.getElementById("mark-block-dialog");
        const custEl = document.getElementById("mark-customer-id");
        const priceEl = document.getElementById("mark-offer-price");
        const customerId = custEl ? custEl.value : null;
        if (!customerId) {
            showBlocksToast("Lütfen bir müşteri seçin.", false);
            return;
        }
        const price = priceEl ? parseFloat(priceEl.value) : null;
        if (!pendingMarkBlockId) return;
        saveMarkBlock(pendingMarkBlockId, customerId, price);
        if (dialog) dialog.close();
        pendingMarkBlockId = null;
    });

    document.querySelectorAll("#mark-block-dialog button[value='cancel'], #mark-cancel")
        .forEach(function (btn) {
            btn.addEventListener("click", function () {
                btn.closest("dialog")?.close();
            });
        });
});

let pendingMarkBlockId = null;

function openMarkBlockModal(id, code, defaultPrice, currentCustomerId) {
    pendingMarkBlockId = id;
    const dialog = document.getElementById("mark-block-dialog");
    const codeEl = document.getElementById("mark-block-code");
    const custEl = document.getElementById("mark-customer-id");
    const priceEl = document.getElementById("mark-offer-price");
    if (codeEl) codeEl.textContent = code;
    if (custEl) custEl.value = currentCustomerId || "";
    if (priceEl) priceEl.value = (defaultPrice && defaultPrice > 0) ? defaultPrice : "";
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksToast("Markalama penceresi açılamadı.", false);
}

function saveMarkBlock(id, customerId, offerPrice) {
    const body = new URLSearchParams();
    body.append("customerId", customerId);
    if (offerPrice) body.append("offerPrice", offerPrice);
    fetch(`/blocks/${id}/api/mark`, {
        method: "POST",
        headers: csrfHeaders(),
        body: body
    }).then(res => handleBlockActionResponse(res, "Blok müşteriye başarıyla markalandı (rezerve edildi)."));
}

window.openMarkBlockModal = openMarkBlockModal;
