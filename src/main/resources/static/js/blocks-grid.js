// Tabulator 6 Data Grid for quarry block production
let blocksTable;
let pendingSellBlockId = null;
let pendingTransferBlockId = null;
let pendingMoveBlockId = null;
let pendingMoveTargetType = null;

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
                            : row.locationType === "FACTORY_BLOCK_YARD"
                                ? "bg-indigo-100 text-indigo-800"
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
            {title: "Taş Cinsi", field: "stoneType", minWidth: 110},
            {
                title: "Tonaj",
                minWidth: 130,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const warn = row.weightDeviationWarning;
                    return `<div>
                        <strong>${gridNumber(row.approximateTonnage)} / ${gridNumber(row.actualTonnage)} t</strong>
                        ${warn ? '<div class="text-xs text-rose-700 font-semibold">%5 sapma uyarısı</div>' : ""}
                    </div>`;
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
                title: "Maliyet",
                field: "totalCost",
                minWidth: 110,
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
                        items.push({icon: "handshake", label: "Sat", onclick: "openSellBlock(" + row.id + ")"});
                        items.push({icon: "truck", label: "Fabrikaya sevk", onclick: "openTransferBlock(" + row.id + ")"});
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

function openSellBlock(id) {
    pendingSellBlockId = id;
    const dialog = document.getElementById("sell-block-dialog");
    const select = document.getElementById("sell-customer-id");
    if (select) select.selectedIndex = 0;
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksToast("Satış penceresi açılamadı.", false);
}

function sellBlock(id, customerId) {
    fetch(`/blocks/${id}/sell`, {
        method: "POST",
        headers: csrfHeaders(),
        body: `customerId=${encodeURIComponent(customerId)}`
    }).then(res => handleBlockActionResponse(res, "Satış kaydedildi."));
}

document.addEventListener("DOMContentLoaded", function () {
    initBlocksGrid();

    document.getElementById("confirm-sell-block")?.addEventListener("click", function () {
        const customerId = document.getElementById("sell-customer-id")?.value;
        const dialog = document.getElementById("sell-block-dialog");
        if (!customerId || !pendingSellBlockId) return;
        sellBlock(pendingSellBlockId, customerId);
        if (dialog) dialog.close();
        pendingSellBlockId = null;
    });

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

    document.querySelectorAll(
        "#sell-block-dialog button[value='cancel'], #move-block-dialog button[value='cancel'], #transfer-cancel"
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
