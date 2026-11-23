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
    var node = button.parentElement;
    while (node) {
        if (node._x_dataStack && node._x_dataStack.length) {
            var data = node._x_dataStack[0];
            if (typeof data.confirmDelete === "function") {
                data.confirmDelete(id, name);
                return;
            }
            if (typeof data.confirmDeleteProject === "function") {
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
