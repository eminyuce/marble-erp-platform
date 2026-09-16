// Tabulator 6 Data Grid for quarry block production
let blocksTable;
let pendingTransferBlockId = null;
let pendingMoveBlockId = null;
let pendingMoveTargetType = null;
let pendingDeleteBlockId = null;

function quarryPage() {
    return {
        activeTab: "blocks",
        yardFilter: "",
        statusFilter: "",
        init() {
            window.quarryPageState = this;
        },
        showBlocksTab() {
            this.activeTab = "blocks";
            this.statusFilter = "";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        setYardFilter(filter) {
            this.activeTab = "blocks";
            this.yardFilter = filter;
            this.statusFilter = "";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        showSoldTab() {
            this.activeTab = "sold";
            this.yardFilter = "";
            this.statusFilter = "SOLD";
            if (typeof reloadBlocksGrid === "function") {
                reloadBlocksGrid();
            }
        },
        gridExtraQuery() {
            let q = "";
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
            updateBlocksGridTotals(response);
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
                minWidth: 140,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const sold = row.canonicalStatus === "SOLD" || row.status === "SOLD";
                    if (!sold) return '<span class="text-slate-400">—</span>';
                    const name = gridText(cell.getValue());
                    return name && name !== "—"
                        ? `<span class="font-semibold text-emerald-800">${name}</span>`
                        : '<span class="text-slate-400">Belirtilmedi</span>';
                }
            },
            {title: "Ocak", field: "quarryName", minWidth: 120},
            {title: "Taş Cinsi", field: "stoneType", minWidth: 100, formatter: (cell) => gridText(cell.getValue())},
            {title: "Seleksiyon", field: "colorTone", minWidth: 100, formatter: (cell) => gridText(cell.getValue())},
            {
                title: "Tonaj",
                field: "approximateTonnage",
                minWidth: 110,
                bottomCalc: blocksMetaCalc("totalTonnage"),
                bottomCalcFormatter: function (cell) {
                    return `<strong>${formatBlocksMetric(cell.getValue(), "t")}</strong>`;
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
                bottomCalc: blocksMetaCalc("totalSurfaceM2"),
                bottomCalcFormatter: function (cell) {
                    return `<strong>${formatBlocksMetric(cell.getValue(), "m²")}</strong>`;
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
                minWidth: 120,
                formatter: function (cell) {
                    return `<span class="px-2 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-700">${gridText(cell.getValue())}</span>`;
                }
            },
            {
                title: "Çıkarma Maliyeti",
                field: "calculatedExtractionCost",
                minWidth: 130,
                bottomCalc: blocksMetaCalc("totalExtractionCost"),
                bottomCalcFormatter: function (cell) {
                    return `<strong>${gridMoney(cell.getValue())}</strong>`;
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
                bottomCalc: blocksMetaCalc("totalCost"),
                bottomCalcFormatter: function (cell) {
                    return `<strong>${gridMoney(cell.getValue())}</strong>`;
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
                    const atQuarry = !sold && (row.canonicalStatus === "PRODUCED" || row.canonicalStatus === "MARKED"
                        || row.status === "QUARRY" || row.status === "PRODUCED" || row.status === "MARKED");
                    if (atQuarry) {
                        if (row.locationType !== "PRODUCTION_YARD") {
                            items.push({
                                icon: "pickaxe",
                                label: "Üretim Sahasına taşı",
                                onclick: "openMoveBlock(" + row.id + ", 'PRODUCTION_YARD', 'Üretim Sahasına taşı')"
                            });
                        }
                        if (row.locationType !== "DISPATCH_YARD") {
                            items.push({
                                icon: "warehouse",
                                label: "Stok Sahasına taşı",
                                onclick: "openMoveBlock(" + row.id + ", 'DISPATCH_YARD', 'Stok Sahasına taşı')"
                            });
                        }
                        items.push({icon: "handshake", label: "Sat", href: "/blocks/" + row.id + "/sell"});
                        items.push({icon: "truck", label: "Fabrikaya sevk", onclick: "openTransferBlock(" + row.id + ")"});
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
    blocksTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });
}

function reloadBlocksGrid() {
    if (blocksTable) {
        blocksTable.setPage(1);
    }
}

let blocksGridMeta = {};

function blocksMetaCalc(key) {
    return function () {
        const value = blocksGridMeta ? blocksGridMeta[key] : null;
        const number = Number(value);
        return Number.isFinite(number) ? number : 0;
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

function updateBlocksGridTotals(response) {
    const meta = response && response.meta ? response.meta : {};
    blocksGridMeta = meta;
    const totalsEl = document.getElementById("blocks-grid-totals");
    const tonEl = document.getElementById("blocks-total-tonnage");
    const m2El = document.getElementById("blocks-total-m2");
    const extractionEl = document.getElementById("blocks-total-extraction");
    const costEl = document.getElementById("blocks-total-cost");
    if (totalsEl) {
        totalsEl.classList.remove("hidden");
        if (tonEl) {
            tonEl.textContent = formatBlocksMetric(meta.totalTonnage, "ton");
        }
        if (m2El) {
            m2El.textContent = formatBlocksMetric(meta.totalSurfaceM2, "m²");
        }
        if (extractionEl) {
            extractionEl.textContent = gridMoney(meta.totalExtractionCost);
        }
        if (costEl) {
            costEl.textContent = gridMoney(meta.totalCost);
        }
    }
    window.setTimeout(function () {
        if (blocksTable && typeof blocksTable.recalc === "function") {
            blocksTable.recalc();
        }
    }, 0);
}

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

function openTransferBlock(id) {
    pendingTransferBlockId = id;
    const dialog = document.getElementById("transfer-block-dialog");
    const costInput = document.getElementById("transfer-cost");
    if (costInput) costInput.value = "";
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
    }
}

function transferBlock(id, cost) {
    fetch(`/blocks/${id}/api/transfer-to-factory`, {
        method: "POST",
        headers: csrfHeaders(),
        body: `transportCost=${encodeURIComponent(cost)}`
    }).then(res => handleBlockActionResponse(res, "Blok fabrikaya sevk edildi."));
}

function openMoveBlock(id, targetType, title) {
    pendingMoveBlockId = id;
    pendingMoveTargetType = targetType;
    const dialog = document.getElementById("move-block-dialog");
    const titleEl = document.getElementById("move-block-title");
    const descInput = document.getElementById("move-description");
    if (titleEl) titleEl.textContent = title || "Saha taşı";
    if (descInput) descInput.value = "";
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
    }
}

function moveBlock(id, targetType, description) {
    fetch(`/blocks/${id}/api/move`, {
        method: "POST",
        headers: csrfHeaders(),
        body: `targetType=${encodeURIComponent(targetType)}&description=${encodeURIComponent(description || "Saha hareketi")}`
    }).then(res => handleBlockActionResponse(res, "Saha güncellendi."));
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

document.addEventListener("DOMContentLoaded", function () {
    initBlocksGrid();

    document.getElementById("confirm-transfer-block")?.addEventListener("click", function () {
        const cost = document.getElementById("transfer-cost")?.value;
        const dialog = document.getElementById("transfer-block-dialog");
        if (!cost || !pendingTransferBlockId) {
            showBlocksToast("Nakliye bedeli girin.", false);
            return;
        }
        transferBlock(pendingTransferBlockId, cost);
        if (dialog) dialog.close();
        pendingTransferBlockId = null;
    });

    document.getElementById("confirm-delete-block")?.addEventListener("click", function () {
        const dialog = document.getElementById("delete-block-dialog");
        if (!pendingDeleteBlockId) return;
        deleteBlock(pendingDeleteBlockId);
        if (dialog) dialog.close();
        pendingDeleteBlockId = null;
    });

    document.querySelectorAll(
        "#sell-block-dialog button[value='cancel'], #move-block-dialog button[value='cancel'], #transfer-cancel, #delete-cancel"
    ).forEach(function (btn) {
        btn.addEventListener("click", function () {
            btn.closest("dialog")?.close();
        });
    });

    document.getElementById("confirm-move-block")?.addEventListener("click", function () {
        const description = document.getElementById("move-description")?.value || "";
        const dialog = document.getElementById("move-block-dialog");
        if (!pendingMoveBlockId || !pendingMoveTargetType) return;
        moveBlock(pendingMoveBlockId, pendingMoveTargetType, description);
        if (dialog) dialog.close();
        pendingMoveBlockId = null;
        pendingMoveTargetType = null;
    });
});
