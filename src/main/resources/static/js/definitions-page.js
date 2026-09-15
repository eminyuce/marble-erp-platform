function definitionPageRoot() {
    return document.querySelector("[data-definition-page]");
}

function definitionAlpine() {
    const root = definitionPageRoot();
    if (root && window.Alpine && typeof Alpine.$data === "function") {
        return Alpine.$data(root);
    }
    return null;
}

function definitionRowById(id) {
    const table = window.definitionTable;
    if (!table || typeof table.getData !== "function") {
        return null;
    }
    return table.getData().find(function (row) {
        return Number(row.id) === Number(id);
    }) || null;
}

function confirmDefinitionDelete(id) {
    const page = definitionAlpine();
    const row = definitionRowById(id);
    if (!page || typeof page.confirmDelete !== "function") {
        return;
    }
    const name = row
        ? (row.companyName || row.name || row.code || row.supplierCode || row.customerCode || String(id))
        : String(id);
    page.confirmDelete(id, name);
}

function csrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = {};
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

function toggleDefinitionActive(url) {
    fetch(url, {method: "POST", headers: csrfHeaders()}).then(function (res) {
        if (res.ok || res.redirected) {
            if (window.definitionTable) {
                window.definitionTable.replaceData();
            }
        }
    });
}

function businessUnitBadge(unit, label) {
    const colors = {
        QUARRY: "bg-emerald-100 text-emerald-800",
        FACTORY: "bg-amber-100 text-amber-800",
        WORKSHOP: "bg-rose-100 text-rose-800",
        SITE: "bg-cyan-100 text-cyan-800"
    };
    return `<span class="px-2 py-0.5 rounded-full text-[11px] font-bold ${colors[unit] || "bg-slate-100 text-slate-700"}">${gridText(label || unit)}</span>`;
}

function bindGridSelectFilter(table, selectId) {
    const select = document.getElementById(selectId);
    if (!table || !select) {
        return;
    }
    select.addEventListener("change", function () {
        table.replaceData();
    });
}

window.definitionPageRoot = definitionPageRoot;
window.definitionAlpine = definitionAlpine;
window.confirmDefinitionDelete = confirmDefinitionDelete;
window.toggleDefinitionActive = toggleDefinitionActive;
window.businessUnitBadge = businessUnitBadge;
window.bindGridSelectFilter = bindGridSelectFilter;
