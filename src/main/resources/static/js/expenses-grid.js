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
                        onclick: "openViewExpense(" + row.id + ")"
                    });
                    if (row.canEdit) {
                        items.push({
                            icon: "edit-3",
                            label: "Düzenle",
                            onclick: "openEditExpense(" + row.id + ")"
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

// Modal Dialog Management
let currentEditingId = null;

async function loadUnitOptions(unit, selectedCenterId, selectedType) {
    const centerSelect = document.getElementById("expense-form-centerId");
    const typeSelect = document.getElementById("expense-form-expenseType");
    if (!centerSelect || !typeSelect) return;

    try {
        const resp = await fetch("/expenses/api/options?unit=" + encodeURIComponent(unit || ""));
        if (!resp.ok) return;
        const data = await resp.json();

        // Populate centers
        centerSelect.innerHTML = '<option value="" disabled selected>Masraf merkezi seçin</option>';
        (data.centers || []).forEach(c => {
            const opt = document.createElement("option");
            opt.value = c.id;
            opt.textContent = `${c.code} — ${c.name}`;
            if (selectedCenterId && String(selectedCenterId) === String(c.id)) {
                opt.selected = true;
            }
            centerSelect.appendChild(opt);
        });

        // Populate types
        typeSelect.innerHTML = '<option value="" disabled selected>Gider kalemi seçin</option>';
        (data.types || []).forEach(t => {
            const opt = document.createElement("option");
            opt.value = t.name;
            opt.textContent = t.label;
            if (selectedType && selectedType === t.name) {
                opt.selected = true;
            }
            typeSelect.appendChild(opt);
        });
    } catch (e) {
        console.error("Failed to load unit options", e);
    }
}

function onModalUnitChange() {
    const unitSelect = document.getElementById("expense-form-businessUnit");
    const quarryGroup = document.getElementById("expense-form-quarry-group");
    const projectGroup = document.getElementById("expense-form-project-group");
    const quarrySelect = document.getElementById("expense-form-quarryId");
    const projectSelect = document.getElementById("expense-form-projectId");

    const unit = unitSelect ? unitSelect.value : "";
    if (quarryGroup) {
        quarryGroup.style.display = unit === "QUARRY" ? "block" : "none";
        if (quarrySelect) quarrySelect.required = unit === "QUARRY";
    }
    if (projectGroup) {
        projectGroup.style.display = unit === "SITE" ? "block" : "none";
        if (projectSelect) projectSelect.required = unit === "SITE";
    }
    loadUnitOptions(unit);
}

function openCreateExpense() {
    currentEditingId = null;
    const dialog = document.getElementById("expense-modal-dialog");
    const form = document.getElementById("expense-form");
    const title = document.getElementById("expense-modal-title");
    const submitBtn = document.getElementById("expense-form-submit");

    if (!dialog || !form) return;
    form.reset();
    if (title) title.textContent = "Yeni Gider Kaydı";
    if (submitBtn) submitBtn.textContent = "Gideri Kaydet";

    // Set default dates
    const today = new Date().toISOString().split("T")[0];
    const month = today.substring(0, 7);
    const dateInput = document.getElementById("expense-form-entryDate");
    const periodInput = document.getElementById("expense-form-expensePeriod");
    if (dateInput) dateInput.value = today;
    if (periodInput) periodInput.value = month;

    // Set initial unit based on page active tab
    const unitSelect = document.getElementById("expense-form-businessUnit");
    const defaultUnit = window.expensesPageState && window.expensesPageState.activeTab
        ? window.expensesPageState.activeTab
        : "QUARRY";
    if (unitSelect) unitSelect.value = defaultUnit;

    onModalUnitChange();
    dialog.showModal();
}

async function openEditExpense(id) {
    currentEditingId = id;
    const dialog = document.getElementById("expense-modal-dialog");
    const title = document.getElementById("expense-modal-title");
    const submitBtn = document.getElementById("expense-form-submit");

    if (!dialog) return;

    try {
        const resp = await fetch("/expenses/api/" + id);
        if (!resp.ok) throw new Error("Gider bilgisi alınamadı.");
        const data = await resp.json();

        if (title) title.textContent = "Gider Kaydını Düzenle";
        if (submitBtn) submitBtn.textContent = "Değişiklikleri Kaydet";

        const unitSelect = document.getElementById("expense-form-businessUnit");
        if (unitSelect) unitSelect.value = data.businessUnit || "QUARRY";

        const quarryGroup = document.getElementById("expense-form-quarry-group");
        const projectGroup = document.getElementById("expense-form-project-group");
        const quarrySelect = document.getElementById("expense-form-quarryId");
        const projectSelect = document.getElementById("expense-form-projectId");

        if (quarryGroup) quarryGroup.style.display = data.businessUnit === "QUARRY" ? "block" : "none";
        if (projectGroup) projectGroup.style.display = data.businessUnit === "SITE" ? "block" : "none";
        if (quarrySelect && data.quarryId) quarrySelect.value = data.quarryId;
        if (projectSelect && data.projectId) projectSelect.value = data.projectId;

        await loadUnitOptions(data.businessUnit, data.centerId, data.expenseType);

        const amountInput = document.getElementById("expense-form-amount");
        const entryDateInput = document.getElementById("expense-form-entryDate");
        const docNoInput = document.getElementById("expense-form-documentNo");
        const invDateInput = document.getElementById("expense-form-invoiceDate");
        const periodInput = document.getElementById("expense-form-expensePeriod");
        const descInput = document.getElementById("expense-form-description");

        if (amountInput) amountInput.value = data.amount || "";
        if (entryDateInput) entryDateInput.value = data.entryDate || "";
        if (docNoInput) docNoInput.value = data.documentNo || "";
        if (invDateInput) invDateInput.value = data.invoiceDate || "";
        if (periodInput) periodInput.value = data.expensePeriod || "";
        if (descInput) descInput.value = data.description || "";

        dialog.showModal();
    } catch (e) {
        showExpensesToast(e.message || "Gider detayları yüklenemedi.", false);
    }
}

async function openViewExpense(id) {
    const dialog = document.getElementById("view-expense-dialog");
    if (!dialog) return;

    try {
        const resp = await fetch("/expenses/api/" + id);
        if (!resp.ok) throw new Error("Gider detayları alınamadı.");
        const data = await resp.json();

        document.getElementById("view-exp-unit").textContent = data.businessUnitLabel || data.businessUnit || "—";
        document.getElementById("view-exp-target").textContent = data.targetName || "—";
        document.getElementById("view-exp-center").textContent = (data.costCenterName || "") + (data.costCenterCode ? ` (${data.costCenterCode})` : "");
        document.getElementById("view-exp-type").textContent = data.expenseTypeLabel || "—";
        document.getElementById("view-exp-date").textContent = data.entryDate || "—";
        document.getElementById("view-exp-invdate").textContent = data.invoiceDate || "—";
        document.getElementById("view-exp-docno").textContent = data.documentNo || "—";
        document.getElementById("view-exp-period").textContent = data.expensePeriod || "—";
        document.getElementById("view-exp-amount").textContent = Number(data.amount || 0).toLocaleString("tr-TR", {minimumFractionDigits: 2}) + " " + (data.currency || "TL");
        document.getElementById("view-exp-desc").textContent = data.description || "—";

        dialog.showModal();
    } catch (e) {
        showExpensesToast(e.message || "Detaylar yüklenemedi.", false);
    }
}

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

async function handleExpenseFormSubmit(e) {
    e.preventDefault();
    const form = document.getElementById("expense-form");
    const dialog = document.getElementById("expense-modal-dialog");
    if (!form) return;

    const formData = new FormData(form);
    const url = currentEditingId ? `/expenses/api/${currentEditingId}/edit` : "/expenses/api";

    try {
        const resp = await fetch(url, {
            method: "POST",
            headers: csrfHeaders(),
            body: new URLSearchParams(formData)
        });
        const data = await resp.json();
        const serviceStatus = data && data.serviceStatus ? data.serviceStatus : {};
        const isOk = serviceStatus.httpStatus === "OK" || (data.status && data.status.errorCode === "0");

        if (isOk) {
            const msg = currentEditingId ? "Gider kaydı güncellendi." : "Gider kaydı oluşturuldu.";
            showExpensesToast(msg, true);
            if (dialog) dialog.close();
            reloadExpensesGrid();
        } else {
            const msg = (serviceStatus.status && serviceStatus.status.message) ? serviceStatus.status.message : "İşlem başarısız.";
            showExpensesToast(msg, false);
        }
    } catch (err) {
        showExpensesToast("Hata oluştu: " + err.message, false);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    initExpensesGrid();

    const form = document.getElementById("expense-form");
    if (form) {
        form.addEventListener("submit", handleExpenseFormSubmit);
    }

    const unitSelect = document.getElementById("expense-form-businessUnit");
    if (unitSelect) {
        unitSelect.addEventListener("change", onModalUnitChange);
    }

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
window.openCreateExpense = openCreateExpense;
window.openEditExpense = openEditExpense;
window.openViewExpense = openViewExpense;
window.openDeleteExpense = openDeleteExpense;
window.confirmDeleteExpense = confirmDeleteExpense;
window.onModalUnitChange = onModalUnitChange;
