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

function positionGridColumnFilterPanel(triggerRect, menuSize, viewport) {
    var gap = 4;
    var left = triggerRect.left;
    var top = triggerRect.bottom + gap;
    if (left + menuSize.width > viewport.width - gap) {
        left = Math.max(gap, viewport.width - menuSize.width - gap);
    }
    if (left < gap) {
        left = gap;
    }
    if (top + menuSize.height > viewport.height - gap
            && triggerRect.top - gap - menuSize.height >= gap) {
        top = triggerRect.top - gap - menuSize.height;
    }
    if (top < gap) {
        top = gap;
    }
    return { left: left, top: top };
}

function closeGridColumnFilter() {
    if (!activeGridColumnFilter) {
        return;
    }
    var panel = activeGridColumnFilter.panel;
    var button = activeGridColumnFilter.button;
    if (panel && panel.parentNode) {
        panel.parentNode.removeChild(panel);
    }
    if (button) {
        button.setAttribute("aria-expanded", "false");
        button.focus();
    }
    document.removeEventListener("mousedown", onGridColumnFilterPointer, true);
    document.removeEventListener("keydown", onGridColumnFilterKey, true);
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
}

function loadGridColumnFilterValues(url, list, input, operator, form) {
    list.textContent = "Yükleniyor…";
    fetch(url, { credentials: "same-origin", headers: { "Accept": "application/json" } })
        .then(function (response) { return response.ok ? response.json() : []; })
        .then(function (values) { fillGridColumnFilterValues(list, values, input, operator, form); })
        .catch(function () { fillGridColumnFilterValues(list, [], input, operator, form); });
}

function toggleGridColumnFilter(event, button) {
    event.preventDefault();
    event.stopPropagation();
    if (activeGridColumnFilter && activeGridColumnFilter.button === button) {
        closeGridColumnFilter();
        return;
    }
    closeGridColumnFilter();
    var template = button.parentElement.querySelector(".erp-data-grid__colfilter-tpl");
    if (!template) {
        return;
    }
    var panel = template.content.firstElementChild.cloneNode(true);
    panel.classList.add("erp-data-grid__colfilter-panel");
    panel.style.visibility = "hidden";
    document.body.appendChild(panel);
    var placed = positionGridColumnFilterPanel(
        button.getBoundingClientRect(),
        { width: panel.offsetWidth, height: panel.offsetHeight },
        { width: window.innerWidth, height: window.innerHeight }
    );
    panel.style.left = placed.left + "px";
    panel.style.top = placed.top + "px";
    panel.style.visibility = "visible";
    button.setAttribute("aria-expanded", "true");
    var input = panel.querySelector("input[type='search']");
    var operator = panel.querySelector("select");
    var list = panel.querySelector(".erp-data-grid__colfilter-values");
    var form = panel;
    if (list && button.getAttribute("data-values-url")) {
        loadGridColumnFilterValues(button.getAttribute("data-values-url"), list, input, operator, form);
    }
    if (input) {
        input.focus();
        input.select();
    }
    activeGridColumnFilter = { button: button, panel: panel };
    document.addEventListener("mousedown", onGridColumnFilterPointer, true);
    document.addEventListener("keydown", onGridColumnFilterKey, true);
    if (window.lucide) {
        window.lucide.createIcons();
    }
}

window.toggleGridColumnFilter = toggleGridColumnFilter;
window.closeGridColumnFilter = closeGridColumnFilter;
