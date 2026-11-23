function pinFrozenColumns(root) {
    if (!root) {
        return;
    }
    var frozen = Number(root.getAttribute("data-frozen") || "0");
    var table = root.querySelector("table");
    if (!table || frozen <= 0) {
        return;
    }
    var headers = table.querySelectorAll("thead th");
    var offsets = [];
    var left = 0;
    for (var index = 0; index < headers.length; index++) {
        if (index < frozen) {
            offsets[index] = left;
            left += headers[index].getBoundingClientRect().width;
        }
    }
    var rows = table.querySelectorAll("tr");
    rows.forEach(function (row) {
        var cells = row.children;
        for (var cellIndex = 0; cellIndex < frozen && cellIndex < cells.length; cellIndex++) {
            cells[cellIndex].style.left = offsets[cellIndex] + "px";
        }
    });
}

function erpDataGrid(el) {
    return {
        goPage: el.getAttribute("data-page") || "1",
        init() {
            var root = el;
            var pin = function () {
                pinFrozenColumns(root);
            };
            pin();
            window.addEventListener("resize", pin);
            if (window.ResizeObserver) {
                var observer = new ResizeObserver(pin);
                observer.observe(root);
            }
        },
        setSize(event) {
            var url = new URL(window.location.href);
            url.searchParams.set("size", event.target.value);
            url.searchParams.set("page", "1");
            window.location.assign(url.toString());
        },
        go() {
            var max = parseInt(el.getAttribute("data-pages") || "1", 10);
            if (!max || max < 1) {
                max = 1;
            }
            var page = parseInt(this.goPage, 10);
            if (!page || page < 1) {
                page = 1;
            }
            if (page > max) {
                page = max;
            }
            var url = new URL(window.location.href);
            url.searchParams.set("page", String(page));
            window.location.assign(url.toString());
        },
        selectRow(event) {
            if (event.target.closest("a, button, summary")) {
                return;
            }
            var row = event.target.closest("tbody tr");
            if (!row || row.querySelector(".erp-data-grid__empty")) {
                return;
            }
            var selected = row.parentElement.querySelectorAll("tr.is-selected");
            selected.forEach(function (item) {
                if (item !== row) {
                    item.classList.remove("is-selected");
                }
            });
            row.classList.toggle("is-selected");
        }
    };
}

function erpGridDelete(button) {
    var id = button.getAttribute("data-delete-id");
    var name = button.getAttribute("data-delete-name") || "";
    var portal = document.getElementById("grid-actions-portal");
    var start = button;
    if (portal && portal.contains(button) && portal._triggerBtn) {
        start = portal._triggerBtn;
    }
    var node = start;
    while (node) {
        if (node._x_dataStack && node._x_dataStack.length) {
            var data = node._x_dataStack[0];
            if (typeof data.confirmDelete === "function") {
                if (typeof closeGridActionsMenu === "function") {
                    closeGridActionsMenu(false);
                }
                data.confirmDelete(id, name);
                return;
            }
            if (typeof data.confirmDeleteProject === "function") {
                if (typeof closeGridActionsMenu === "function") {
                    closeGridActionsMenu(false);
                }
                data.confirmDeleteProject(id, name);
                return;
            }
        }
        node = node.parentElement;
    }
}

function erpSubmitSearch(event) {
    if (event) {
        event.preventDefault();
    }
    var form = event ? event.target : null;
    var input = form ? form.querySelector("input[type='search'], input[name='search']") : null;
    var url = new URL(window.location.href);
    url.searchParams.set("search", input ? input.value : "");
    url.searchParams.set("page", "1");
    window.location.assign(url.toString());
    return false;
}

window.pinFrozenColumns = pinFrozenColumns;
window.erpDataGrid = erpDataGrid;
window.erpGridDelete = erpGridDelete;
window.erpSubmitSearch = erpSubmitSearch;

var activeGridColumnFilter = null;
var GRID_FILTER_GAP = 4;
var GRID_FILTER_MARGIN = 8;

function positionGridColumnFilterPanel(triggerRect, menuSize, viewport) {
    var placer = window.positionGridActionsMenu;
    if (typeof placer === "function") {
        return placer(triggerRect, menuSize, viewport);
    }
    var left = triggerRect.left;
    var top = triggerRect.bottom + GRID_FILTER_GAP;
    if (top + menuSize.height > viewport.height - GRID_FILTER_MARGIN) {
        var flipped = triggerRect.top - menuSize.height - GRID_FILTER_GAP;
        if (flipped >= GRID_FILTER_MARGIN) {
            top = flipped;
        } else {
            top = Math.max(GRID_FILTER_MARGIN, viewport.height - menuSize.height - GRID_FILTER_MARGIN);
        }
    }
    if (left + menuSize.width > viewport.width - GRID_FILTER_MARGIN) {
        left = viewport.width - menuSize.width - GRID_FILTER_MARGIN;
    }
    if (left < GRID_FILTER_MARGIN) {
        left = GRID_FILTER_MARGIN;
    }
    if (top < GRID_FILTER_MARGIN) {
        top = GRID_FILTER_MARGIN;
    }
    return {left: left, top: top};
}

function placeOpenGridColumnFilter() {
    if (!activeGridColumnFilter) {
        return;
    }
    var button = activeGridColumnFilter.button;
    var panel = activeGridColumnFilter.panel;
    if (!button.isConnected || !panel.isConnected) {
        closeGridColumnFilter(false);
        return;
    }
    var rect = button.getBoundingClientRect();
    if (rect.bottom < 0 || rect.top > window.innerHeight
            || rect.right < 0 || rect.left > window.innerWidth) {
        closeGridColumnFilter(false);
        return;
    }
    var placed = positionGridColumnFilterPanel(
        rect,
        {width: panel.offsetWidth, height: panel.offsetHeight},
        {width: window.innerWidth, height: window.innerHeight}
    );
    panel.style.position = "fixed";
    panel.style.right = "auto";
    panel.style.left = placed.left + "px";
    panel.style.top = placed.top + "px";
    panel.style.zIndex = "9999";
}

function onGridColumnFilterReposition() {
    placeOpenGridColumnFilter();
}

function bindGridColumnFilterListeners() {
    document.addEventListener("mousedown", onGridColumnFilterPointer, true);
    document.addEventListener("keydown", onGridColumnFilterKey, true);
    document.addEventListener("scroll", onGridColumnFilterReposition, true);
    window.addEventListener("resize", onGridColumnFilterReposition);
}

function unbindGridColumnFilterListeners() {
    document.removeEventListener("mousedown", onGridColumnFilterPointer, true);
    document.removeEventListener("keydown", onGridColumnFilterKey, true);
    document.removeEventListener("scroll", onGridColumnFilterReposition, true);
    window.removeEventListener("resize", onGridColumnFilterReposition);
}

function closeGridColumnFilter(restoreFocus) {
    if (!activeGridColumnFilter) {
        return;
    }
    var panel = activeGridColumnFilter.panel;
    var button = activeGridColumnFilter.button;
    unbindGridColumnFilterListeners();
    if (panel && panel.parentNode) {
        panel.parentNode.removeChild(panel);
    }
    if (button) {
        button.setAttribute("aria-expanded", "false");
        if (restoreFocus !== false) {
            button.focus();
        }
    }
    activeGridColumnFilter = null;
}

function onGridColumnFilterPointer(event) {
    if (!activeGridColumnFilter) {
        return;
    }
    if (activeGridColumnFilter.panel.contains(event.target)
            || activeGridColumnFilter.button.contains(event.target)) {
        return;
    }
    closeGridColumnFilter();
}

function onGridColumnFilterKey(event) {
    if (!activeGridColumnFilter) {
        return;
    }
    if (event.key === "Escape") {
        event.preventDefault();
        closeGridColumnFilter();
        return;
    }
    var options = activeGridColumnFilter.panel.querySelectorAll("[role='option']");
    if (!options.length || (event.key !== "ArrowDown" && event.key !== "ArrowUp" && event.key !== "Enter")) {
        return;
    }
    var current = activeGridColumnFilter.panel.querySelector("[role='option'][aria-selected='true']");
    var index = current ? Array.prototype.indexOf.call(options, current) : -1;
    if (event.key === "Enter" && current) {
        event.preventDefault();
        current.click();
        return;
    }
    if (event.key === "ArrowDown") {
        event.preventDefault();
        index = Math.min(options.length - 1, index + 1);
    } else if (event.key === "ArrowUp") {
        event.preventDefault();
        index = Math.max(0, index - 1);
    }
    options.forEach(function (option) { option.setAttribute("aria-selected", "false"); });
    options[index].setAttribute("aria-selected", "true");
    options[index].focus();
}

function fillGridColumnFilterValues(list, values, input, operator, form) {
    list.innerHTML = "";
    if (!values || !values.length) {
        var empty = document.createElement("div");
        empty.className = "erp-data-grid__colfilter-empty";
        empty.textContent = "Değer yok";
        list.appendChild(empty);
        placeOpenGridColumnFilter();
        return;
    }
    values.forEach(function (item) {
        var option = document.createElement("button");
        option.type = "button";
        option.setAttribute("role", "option");
        option.setAttribute("aria-selected", "false");
        option.textContent = item.label || item.value;
        option.addEventListener("click", function () {
            input.value = item.value;
            operator.value = "equals";
            form.submit();
        });
        list.appendChild(option);
    });
    placeOpenGridColumnFilter();
}

function loadGridColumnFilterValues(url, list, input, operator, form) {
    list.textContent = "Yükleniyor…";
    fetch(url, { credentials: "same-origin", headers: { "Accept": "application/json" } })
        .then(function (response) { return response.ok ? response.json() : []; })
        .then(function (values) { fillGridColumnFilterValues(list, values, input, operator, form); })
        .catch(function () { fillGridColumnFilterValues(list, [], input, operator, form); });
}

function syncGridColumnFilterRange(select) {
    if (!select) {
        return;
    }
    var form = select.form || select.closest("form");
    if (!form) {
        return;
    }
    var toField = form.querySelector(".erp-data-grid__colfilter-to");
    if (!toField) {
        return;
    }
    if (select.value === "between") {
        toField.classList.remove("is-hidden");
    } else {
        toField.classList.add("is-hidden");
    }
    if (typeof placeOpenGridColumnFilter === "function") {
        placeOpenGridColumnFilter();
    }
}

function toggleGridColumnFilter(event, button) {
    event.preventDefault();
    event.stopPropagation();
    if (activeGridColumnFilter && activeGridColumnFilter.button === button) {
        closeGridColumnFilter();
        return;
    }
    closeGridColumnFilter(false);
    var template = button.parentElement.querySelector(".erp-data-grid__colfilter-tpl");
    if (!template) {
        return;
    }
    var panel = template.content.firstElementChild.cloneNode(true);
    panel.classList.add("erp-data-grid__colfilter-panel");
    panel.style.cssText = "position:fixed;visibility:hidden;left:0;top:0;right:auto;z-index:9999;";
    document.body.appendChild(panel);
    button.setAttribute("aria-expanded", "true");
    activeGridColumnFilter = { button: button, panel: panel };
    placeOpenGridColumnFilter();
    panel.style.visibility = "visible";
    var input = panel.querySelector("input[name^='cf.']");
    var operator = panel.querySelector("select");
    var list = panel.querySelector(".erp-data-grid__colfilter-values");
    var form = panel;
    var kind = button.getAttribute("data-filter-kind") || "text";
    if (operator) {
        operator.addEventListener("change", function () {
            syncGridColumnFilterRange(operator);
        });
        syncGridColumnFilterRange(operator);
    }
    if (list && kind === "text" && button.getAttribute("data-values-url")) {
        loadGridColumnFilterValues(button.getAttribute("data-values-url"), list, input, operator, form);
    }
    if (input) {
        input.focus();
        if (typeof input.select === "function") {
            input.select();
        }
    }
    bindGridColumnFilterListeners();
    if (window.lucide) {
        window.lucide.createIcons();
    }
}

window.positionGridColumnFilterPanel = positionGridColumnFilterPanel;
window.placeOpenGridColumnFilter = placeOpenGridColumnFilter;
window.syncGridColumnFilterRange = syncGridColumnFilterRange;
window.toggleGridColumnFilter = toggleGridColumnFilter;
window.closeGridColumnFilter = closeGridColumnFilter;
