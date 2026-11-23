function blockActionCsrfHeaders() {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
    const headers = {"Content-Type": "application/x-www-form-urlencoded"};
    if (csrfHeader && csrfToken) {
        headers[csrfHeader] = csrfToken;
    }
    return headers;
}

function showBlocksPageToast(message, ok) {
    const existing = document.querySelector(".erp-toast");
    if (existing) {
        existing.remove();
    }
    const toast = document.createElement("div");
    toast.className = "erp-toast " + (ok ? "erp-toast--ok" : "erp-toast--err");
    toast.setAttribute("role", "alert");
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(function () {
        toast.remove();
    }, 4000);
}

function isSuccessfulBlockResponse(body) {
    const code = body && body.serviceStatus && body.serviceStatus.status
        ? body.serviceStatus.status.errorCode
        : undefined;
    return code === undefined || code === "0" || code === 0;
}

function handleBlockPageAction(res, okMessage) {
    return res.json().then(function (body) {
        if (res.ok && isSuccessfulBlockResponse(body)) {
            showBlocksPageToast(okMessage, true);
            window.location.reload();
            return;
        }
        showBlocksPageToast("İşlem tamamlanamadı. Lütfen tekrar deneyin veya blok detayından deneyin.", false);
    }).catch(function () {
        if (res.ok) {
            showBlocksPageToast(okMessage, true);
            window.location.reload();
            return;
        }
        showBlocksPageToast("İşlem tamamlanamadı. Lütfen tekrar deneyin veya blok detayından deneyin.", false);
    });
}

let pendingTonnageBlockId = null;
let pendingMarkBlockId = null;

function openActualTonnageModal(id, code, approxTon) {
    pendingTonnageBlockId = id;
    const dialog = document.getElementById("tonnage-modal-dialog");
    const codeEl = document.getElementById("tonnage-block-code");
    const approxEl = document.getElementById("tonnage-approx");
    const inputEl = document.getElementById("input-actual-tonnage");
    if (codeEl) {
        codeEl.textContent = code || ("ID: " + id);
    }
    if (approxEl) {
        approxEl.textContent = (approxTon || 0) + " Ton";
    }
    if (inputEl) {
        inputEl.value = "";
        inputEl.focus();
    }
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksPageToast("Tartım penceresi açılamadı.", false);
}

function openMarkBlockModal(id, code, defaultPrice, currentCustomerId) {
    pendingMarkBlockId = id;
    const dialog = document.getElementById("mark-block-dialog");
    const codeEl = document.getElementById("mark-block-code");
    const custEl = document.getElementById("mark-customer-id");
    const priceEl = document.getElementById("mark-offer-price");
    if (codeEl) {
        codeEl.textContent = code;
    }
    if (custEl) {
        custEl.value = currentCustomerId || "";
    }
    if (priceEl) {
        priceEl.value = (defaultPrice && defaultPrice > 0) ? defaultPrice : "";
    }
    if (dialog && typeof dialog.showModal === "function") {
        dialog.showModal();
        return;
    }
    showBlocksPageToast("Markalama penceresi açılamadı.", false);
}

function saveActualTonnage(id, tonnage) {
    const body = new URLSearchParams();
    body.append("actualTonnage", tonnage);
    fetch("/blocks/" + id + "/api/actual-tonnage", {
        method: "POST",
        headers: blockActionCsrfHeaders(),
        body: body
    }).then(function (res) {
        return handleBlockPageAction(res, "Kantar tartımı kaydedildi (Tartıldı / Sevke Hazır).");
    });
}

function saveMarkBlock(id, customerId, offerPrice) {
    const body = new URLSearchParams();
    body.append("customerId", customerId);
    if (offerPrice) {
        body.append("offerPrice", offerPrice);
    }
    fetch("/blocks/" + id + "/api/mark", {
        method: "POST",
        headers: blockActionCsrfHeaders(),
        body: body
    }).then(function (res) {
        return handleBlockPageAction(res, "Blok müşteriye başarıyla markalandı (rezerve edildi).");
    });
}

document.addEventListener("DOMContentLoaded", function () {
    document.getElementById("confirm-save-tonnage")?.addEventListener("click", function () {
        const dialog = document.getElementById("tonnage-modal-dialog");
        const inputEl = document.getElementById("input-actual-tonnage");
        const val = inputEl ? parseFloat(inputEl.value) : null;
        if (!val || isNaN(val) || val <= 0) {
            showBlocksPageToast("Lütfen geçerli bir kantar tonajı girin.", false);
            return;
        }
        if (!pendingTonnageBlockId) {
            return;
        }
        saveActualTonnage(pendingTonnageBlockId, val);
        if (dialog) {
            dialog.close();
        }
        pendingTonnageBlockId = null;
    });
    document.getElementById("tonnage-cancel")?.addEventListener("click", function () {
        document.getElementById("tonnage-modal-dialog")?.close();
    });
    document.getElementById("confirm-mark-block")?.addEventListener("click", function () {
        const dialog = document.getElementById("mark-block-dialog");
        const custEl = document.getElementById("mark-customer-id");
        const priceEl = document.getElementById("mark-offer-price");
        const customerId = custEl ? custEl.value : null;
        if (!customerId) {
            showBlocksPageToast("Lütfen bir müşteri seçin.", false);
            return;
        }
        const price = priceEl ? parseFloat(priceEl.value) : null;
        if (!pendingMarkBlockId) {
            return;
        }
        saveMarkBlock(pendingMarkBlockId, customerId, price);
        if (dialog) {
            dialog.close();
        }
        pendingMarkBlockId = null;
    });
    document.getElementById("mark-cancel")?.addEventListener("click", function () {
        document.getElementById("mark-block-dialog")?.close();
    });
});

window.openActualTonnageModal = openActualTonnageModal;
window.openMarkBlockModal = openMarkBlockModal;
