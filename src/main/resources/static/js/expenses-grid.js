// Tabulator 6 Data Grid for Expenses & Procurement management
let expensesTable;
let pendingDeleteExpenseId = null;

function expensesPage() {
    return {
        activeTab: "",
        period: "",
        init() {
            window.expensesPageState = this;
            const periodInput = document.getElementById("expenses-period-filter");
            if (periodInput && periodInput.value) {
                this.period = periodInput.value;
            }
        },
        setUnitFilter(unit) {
            this.activeTab = unit;
            reloadExpensesGrid();
        },
        setPeriodFilter(p) {
            this.period = p;
            reloadExpensesGrid();
        },
        gridExtraQuery() {
            let q = "";
            if (this.activeTab) {
                q += "&unit=" + encodeURIComponent(this.activeTab);
            }
            if (this.period) {
                q += "&period=" + encodeURIComponent(this.period);
            }
            return q;
        }
    };
}

function initExpensesGrid() {
    const tableElement = document.getElementById("expenses-table");
    if (!tableElement) return;

    expensesTable = new Tabulator("#expenses-table", {
        ...erpGridDefaults(),
        pagination: true,
        paginationMode: "remote",
        paginationSize: window.ERP_GRID_PAGE_SIZE || 25,
        paginationSizeSelector: [10, 25, 50, 100],
        ajaxURL: "/expenses/api/data",
        ajaxConfig: {
            method: "GET",
            headers: {"Accept": "application/json"}
        },
        ajaxURLGenerator: function (url, config, params) {
            return erpGridAjaxUrl(url, params, "search-input", function () {
                return window.expensesPageState ? window.expensesPageState.gridExtraQuery() : "";
            });
        },
        ajaxResponse: function (url, params, response) {
            updateExpensesGridTotals(response);
            return erpGridAjaxResponse("expenses-table", response);
        },
        placeholder: "Gider kaydı bulunamadı.",
        columns: [
            erpResponsiveCollapseColumn(),
            erpIndexColumn(),
            {
                title: "Tarih",
                field: "entryDate",
                minWidth: 105,
                width: 110,
                responsive: 0,
                formatter: function (cell) {
                    const val = cell.getValue();
                    if (!val) return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono text-xs font-semibold text-slate-700">${gridText(val)}</span>`;
                }
            },
            {
                title: "Belge / Fatura",
                field: "documentNo",
                minWidth: 130,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const doc = gridText(cell.getValue());
                    const invDate = row.invoiceDate ? `<span class="text-[11px] text-slate-400 block">${row.invoiceDate}</span>` : "";
                    if (!doc || doc === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono font-bold text-slate-800">${doc}</span>${invDate}`;
                }
            },
            {
                title: "Birim",
                field: "businessUnit",
                minWidth: 110,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const unit = cell.getValue();
                    const label = row.businessUnitLabel || unit || "—";
                    let badgeClass = "bg-slate-100 text-slate-700";
                    if (unit === "QUARRY") badgeClass = "bg-amber-100 text-amber-800";
                    else if (unit === "FACTORY") badgeClass = "bg-blue-100 text-blue-800";
                    else if (unit === "WORKSHOP") badgeClass = "bg-rose-100 text-rose-800";
                    else if (unit === "SITE") badgeClass = "bg-cyan-100 text-cyan-800";
                    return `<span class="px-2 py-0.5 rounded text-xs font-semibold ${badgeClass}">${label}</span>`;
                }
            },
            {
                title: "İlgili Yer",
                field: "targetName",
                minWidth: 130,
                formatter: function (cell) {
                    const val = gridText(cell.getValue());
                    if (!val || val === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="font-medium text-slate-800">${val}</span>`;
                }
            },
            {
                title: "Masraf Merkezi",
                field: "costCenterName",
                minWidth: 150,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const name = gridText(cell.getValue());
                    const code = row.costCenterCode ? `<span class="font-mono text-[11px] text-slate-400"> (${row.costCenterCode})</span>` : "";
                    if (!name || name === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="text-slate-700 font-medium">${name}</span>${code}`;
                }
            },
            {
                title: "Gider Kalemi",
                field: "expenseTypeLabel",
                minWidth: 140,
                formatter: function (cell) {
                    const val = gridText(cell.getValue());
                    if (!val || val === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="font-semibold text-slate-800">${val}</span>`;
                }
            },
            {
                title: "Dönem",
                field: "expensePeriod",
                minWidth: 90,
                width: 95,
                formatter: function (cell) {
                    const val = gridText(cell.getValue());
                    if (!val || val === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="font-mono text-xs text-slate-600">${val}</span>`;
                }
            },
            {
                title: "Tutar",
                field: "amount",
                minWidth: 120,
                hozAlign: "right",
                headerHozAlign: "right",
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const curr = row.currency || "TL";
                    return `<span class="font-mono font-bold text-slate-900">${gridMoney(cell.getValue())} ${curr}</span>`;
                }
            },
            {
                title: "Açıklama",
                field: "description",
                minWidth: 160,
                formatter: function (cell) {
                    const val = gridText(cell.getValue());
                    if (!val || val === "—") return '<span class="text-slate-400">—</span>';
                    return `<span class="text-xs text-slate-600 truncate max-w-xs block" title="${val}">${val}</span>`;
                }
            },
            {
                title: "İşlemler",
                field: "_actions",
                minWidth: 110,
                width: 115,
                headerSort: false,
                responsive: 0,
                formatter: function (cell) {
                    const row = cell.getRow().getData();
                    const items = [];
                    items.push({
                        icon: "eye",
                        label: "Detay",
                        href: "/expenses/" + row.id
                    });
                    if (row.canEdit) {
                        items.push({
                            icon: "edit-3",
                            label: "Düzenle",
                            href: "/expenses/" + row.id + "/edit"
                        });
                    }
                    if (row.canDelete) {
                        const escapedDoc = (row.documentNo || "").replace(/'/g, "\\'");
                        const formattedAmt = Number(row.amount || 0).toLocaleString("tr-TR", {minimumFractionDigits: 2});
                        items.push({
                            icon: "trash-2",
                            label: "Gideri Sil",
                            danger: true,
                            onclick: "openDeleteExpense(" + row.id + ", '" + escapedDoc + "', '" + formattedAmt + "')"
                        });
                    }
                    return gridActionsHtml(items);
                }
            }
        ]
    });

    attachTabulatorPagingAnimation(expensesTable);
    bindGridSearch(expensesTable, "search-input");
    expensesTable.on("renderComplete", () => {
        if (window.lucide) window.lucide.createIcons();
    });
}

function reloadExpensesGrid() {
    if (expensesTable) {
        expensesTable.setPage(1);
    }
}

function updateExpensesGridTotals(response) {
    const meta = response && response.meta ? response.meta : null;
    const totalsEl = document.getElementById("expenses-grid-totals");
    const amountEl = document.getElementById("expenses-total-amount");
    const countEl = document.getElementById("expenses-total-count");
    if (!totalsEl || !amountEl || !countEl) return;
    if (!meta) {
        totalsEl.classList.add("hidden");
        return;
    }
    totalsEl.classList.remove("hidden");
    amountEl.textContent = meta.totalAmount != null
        ? Number(meta.totalAmount).toLocaleString("tr-TR", {minimumFractionDigits: 2, maximumFractionDigits: 2}) + " TL"
        : "—";
    countEl.textContent = meta.count != null ? meta.count : (response.total || 0);
}

function csrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = {};
    if (csrfHeader && csrfToken) headers[csrfHeader] = csrfToken;
    return headers;
}

function showExpensesToast(message, ok) {
    const existing = document.querySelector(".erp-toast");
    if (existing) existing.remove();
    const toast = document.createElement("div");
    toast.className = "erp-toast " + (ok ? "erp-toast--ok" : "erp-toast--err");
    toast.setAttribute("role", "alert");
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

// Delete Expense Confirmation Management
let pendingDeleteExpenseId = null;

function openDeleteExpense(id, docNo, amount) {
    pendingDeleteExpenseId = id;
    const dialog = document.getElementById("delete-expense-dialog");
    const docEl = document.getElementById("delete-expense-doc");
    const amtEl = document.getElementById("delete-expense-amount");

    if (!dialog) return;
    if (docEl) docEl.textContent = docNo ? `Belge No: ${docNo}` : `Kayıt ID: #${id}`;
    if (amtEl) amtEl.textContent = amount ? `${amount} TL` : "";

    dialog.showModal();
}

async function confirmDeleteExpense() {
    if (!pendingDeleteExpenseId) return;
    const dialog = document.getElementById("delete-expense-dialog");

    try {
        const resp = await fetch(`/expenses/api/${pendingDeleteExpenseId}/delete`, {
            method: "POST",
            headers: csrfHeaders()
        });
        const data = await resp.json();
        const serviceStatus = data && data.serviceStatus ? data.serviceStatus : {};
        const isOk = serviceStatus.httpStatus === "OK" || (data.status && data.status.errorCode === "0");

        if (isOk) {
            showExpensesToast("Gider kaydı başarıyla silindi.", true);
            if (dialog) dialog.close();
            reloadExpensesGrid();
        } else {
            const msg = (serviceStatus.status && serviceStatus.status.message) ? serviceStatus.status.message : "Silme işlemi başarısız.";
            showExpensesToast(msg, false);
        }
    } catch (e) {
        showExpensesToast("İşlem gerçekleştirilemedi: " + e.message, false);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    initExpensesGrid();

    const confirmDeleteBtn = document.getElementById("confirm-delete-expense");
    if (confirmDeleteBtn) {
        confirmDeleteBtn.addEventListener("click", confirmDeleteExpense);
    }

    const periodFilter = document.getElementById("expenses-period-filter");
    if (periodFilter) {
        periodFilter.addEventListener("change", (e) => {
            if (window.expensesPageState) {
                window.expensesPageState.setPeriodFilter(e.target.value);
            }
        });
    }
});

// Explicit window exports
window.initExpensesGrid = initExpensesGrid;
window.reloadExpensesGrid = reloadExpensesGrid;
window.openDeleteExpense = openDeleteExpense;
window.confirmDeleteExpense = confirmDeleteExpense;
