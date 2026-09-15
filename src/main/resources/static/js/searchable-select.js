(function () {
    const ENHANCED_ATTR = "data-erp-searchable";
    const SKIP_ATTR = "data-native-select";
    const OPEN_CLASS = "is-open";
    const ACTIVE_CLASS = "is-active";
    const SELECTED_CLASS = "is-selected";
    const NOUN_BY_NAME = {
        blockId: "blok",
        customerId: "müşteri",
        supplierId: "tedarikçi",
        projectId: "proje",
        quarryId: "ocak",
        slabId: "plaka",
        scrapReason: "neden",
        qualityGrade: "sınıf",
        crackLevel: "seviye",
        locationId: "mahal",
        consumptionType: "tip",
        itemType: "tip",
        templateKey: "şablon",
        "grid.default_page_size": "satır"
    };

    const instances = new WeakMap();
    let openInstance = null;

    function normalizeSearch(value) {
        return String(value || "")
            .toLocaleLowerCase("tr-TR")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "");
    }

    function shouldSkip(select) {
        if (!(select instanceof HTMLSelectElement)) {
            return true;
        }
        if (select.hasAttribute(ENHANCED_ATTR) || select.hasAttribute(SKIP_ATTR)) {
            return true;
        }
        if (select.multiple || select.size > 1) {
            return true;
        }
        if (select.closest(".tabulator") || select.closest("[data-erp-searchable-root]")) {
            return true;
        }
        return false;
    }

    function optionNoun(select) {
        if (select.dataset.noun) {
            return select.dataset.noun;
        }
        const byName = NOUN_BY_NAME[select.name] || NOUN_BY_NAME[select.id];
        if (byName) {
            return byName;
        }
        const placeholder = Array.from(select.options).find((option) => !option.value);
        const match = placeholder && /seçin|seçilmedi/i.test(placeholder.textContent || "")
            ? (placeholder.textContent || "").replace(/\s*(seçin|seçilmedi)\s*/gi, "").trim()
            : "";
        return match ? match.toLocaleLowerCase("tr-TR") : "seçenek";
    }

    function searchPlaceholder(select, noun) {
        return select.dataset.searchPlaceholder || (noun + " ara...");
    }

    function selectedLabel(select) {
        const option = select.options[select.selectedIndex];
        if (!option || (option.disabled && !option.value)) {
            return option ? option.textContent.trim() : "Seçin";
        }
        return option.textContent.trim() || "Seçin";
    }

    function createEl(tag, className, attrs) {
        const el = document.createElement(tag);
        if (className) {
            el.className = className;
        }
        if (attrs) {
            Object.entries(attrs).forEach(([key, value]) => {
                if (value === false || value == null) {
                    return;
                }
                if (key === "text") {
                    el.textContent = value;
                    return;
                }
                if (key === "html") {
                    el.innerHTML = value;
                    return;
                }
                el.setAttribute(key, value === true ? "" : value);
            });
        }
        return el;
    }

    function SearchableSelect(select) {
        this.select = select;
        this.noun = optionNoun(select);
        const rootClasses = ["erp-ss"];
        if (!select.classList.contains("erp-form-select")) {
            rootClasses.push("erp-ss--compact");
        }
        if (select.classList.contains("erp-toolbar-filter")) {
            rootClasses.push("erp-ss--toolbar");
        }
        this.root = createEl("div", rootClasses.join(" "));
        this.root.setAttribute("data-erp-searchable-root", "");
        this.trigger = createEl("button", "erp-ss-trigger", {type: "button", "aria-haspopup": "listbox"});
        this.triggerLabel = createEl("span", "erp-ss-trigger-label");
        this.trigger.append(
            this.triggerLabel,
            createEl("span", "erp-ss-chevron", {
                html: '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M5 7.5 10 12.5 15 7.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>'
            })
        );
        this.panel = createEl("div", "erp-ss-panel", {hidden: true});
        this.searchWrap = createEl("div", "erp-ss-search");
        this.searchInput = createEl("input", "erp-ss-search-input", {
            type: "search",
            placeholder: searchPlaceholder(select, this.noun),
            autocomplete: "off",
            spellcheck: "false"
        });
        this.searchClear = createEl("button", "erp-ss-icon-btn", {type: "button", hidden: true, "aria-label": "Aramayı temizle"});
        this.searchClear.innerHTML = '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M5 5l10 10M15 5 5 15" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';
        this.searchWrap.append(
            createEl("span", "erp-ss-search-icon", {
                html: '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><circle cx="8.5" cy="8.5" r="5.25" stroke="currentColor" stroke-width="1.6"/><path d="m12.5 12.5 4 4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>'
            }),
            this.searchInput,
            this.searchClear
        );
        this.meta = createEl("div", "erp-ss-meta");
        this.count = createEl("span", "erp-ss-count");
        this.meta.append(this.count);
        this.list = createEl("ul", "erp-ss-list", {role: "listbox"});
        this.empty = createEl("div", "erp-ss-empty", {text: "Sonuç bulunamadı", hidden: true});
        this.panel.append(this.searchWrap, this.meta, this.list, this.empty);

        select.setAttribute(ENHANCED_ATTR, "");
        select.classList.add("erp-ss-native");
        select.parentNode.insertBefore(this.root, select);
        this.root.append(select, this.trigger, this.panel);

        this.activeIndex = -1;
        this.filtered = [];
        this.syncTrigger();
        this.bind();
        this.observeOptions();
    }

    SearchableSelect.prototype.bind = function () {
        const self = this;
        this.trigger.addEventListener("click", function (event) {
            event.preventDefault();
            if (self.select.disabled) {
                return;
            }
            self.toggle();
        });
        this.trigger.addEventListener("keydown", function (event) {
            if (event.key === "ArrowDown" || event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                self.open();
            }
        });
        this.searchInput.addEventListener("input", function () {
            self.renderList();
            self.searchClear.hidden = !self.searchInput.value;
        });
        this.searchInput.addEventListener("keydown", function (event) {
            self.onSearchKey(event);
        });
        this.searchClear.addEventListener("click", function () {
            self.searchInput.value = "";
            self.searchClear.hidden = true;
            self.renderList();
            self.searchInput.focus();
        });
        this.list.addEventListener("mousedown", function (event) {
            const item = event.target.closest("[data-value]");
            const clearBtn = event.target.closest("[data-clear]");
            if (clearBtn) {
                event.preventDefault();
                self.choose("");
                return;
            }
            if (item) {
                event.preventDefault();
                self.choose(item.getAttribute("data-value"));
            }
        });
        this.select.addEventListener("change", function () {
            self.syncTrigger();
        });
        this.select.addEventListener("invalid", function () {
            self.open();
        });
        this.select.addEventListener("focus", function () {
            self.trigger.focus();
        });
        const form = this.select.form;
        if (form) {
            form.addEventListener("reset", function () {
                window.setTimeout(function () {
                    self.syncTrigger();
                }, 0);
            });
        }
    };

    SearchableSelect.prototype.observeOptions = function () {
        const self = this;
        this.observer = new MutationObserver(function () {
            self.syncTrigger();
            if (self.root.classList.contains(OPEN_CLASS)) {
                self.renderList();
            }
        });
        this.observer.observe(this.select, {childList: true, subtree: true, attributes: true});
    };

    SearchableSelect.prototype.toggle = function () {
        if (this.root.classList.contains(OPEN_CLASS)) {
            this.close();
        } else {
            this.open();
        }
    };

    SearchableSelect.prototype.open = function () {
        if (this.select.disabled) {
            return;
        }
        if (openInstance && openInstance !== this) {
            openInstance.close();
        }
        openInstance = this;
        this.searchInput.value = "";
        this.searchClear.hidden = true;
        this.renderList();
        this.panel.hidden = false;
        this.root.classList.add(OPEN_CLASS);
        this.trigger.setAttribute("aria-expanded", "true");
        window.setTimeout(() => this.searchInput.focus(), 0);
    };

    SearchableSelect.prototype.close = function () {
        this.panel.hidden = true;
        this.root.classList.remove(OPEN_CLASS);
        this.trigger.setAttribute("aria-expanded", "false");
        if (openInstance === this) {
            openInstance = null;
        }
    };

    SearchableSelect.prototype.syncTrigger = function () {
        const label = selectedLabel(this.select);
        this.triggerLabel.textContent = label;
        this.trigger.classList.toggle("is-placeholder", !this.select.value);
        this.trigger.disabled = this.select.disabled;
        this.root.classList.toggle("is-invalid", this.select.classList.contains("is-invalid"));
        this.root.classList.toggle("is-disabled", this.select.disabled);
        if (this.select.id) {
            this.trigger.id = this.select.id + "-ss-trigger";
        }
    };

    SearchableSelect.prototype.optionEntries = function () {
        return Array.from(this.select.options).map((option, index) => ({
            value: option.value,
            label: (option.textContent || "").trim(),
            disabled: option.disabled,
            selected: option.selected,
            index
        }));
    };

    SearchableSelect.prototype.renderList = function () {
        const query = normalizeSearch(this.searchInput.value);
        const entries = this.optionEntries();
        this.filtered = entries.filter((entry) => !query || normalizeSearch(entry.label).includes(query));
        this.list.innerHTML = "";
        this.filtered.forEach((entry, index) => {
            const item = createEl("li", "erp-ss-option", {
                role: "option",
                "data-value": entry.value,
                "data-index": String(index)
            });
            item.classList.toggle(SELECTED_CLASS, entry.selected);
            item.classList.toggle("is-disabled", entry.disabled && !!entry.value);
            item.textContent = entry.label || "—";
            if (entry.selected && !entry.value) {
                const clear = createEl("button", "erp-ss-icon-btn erp-ss-option-clear", {
                    type: "button",
                    "data-clear": "",
                    "aria-label": "Seçimi temizle"
                });
                clear.innerHTML = '<svg viewBox="0 0 20 20" fill="none" aria-hidden="true"><path d="M5 5l10 10M15 5 5 15" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>';
                item.append(clear);
            }
            this.list.append(item);
        });
        const total = entries.length;
        const shown = this.filtered.length;
        this.count.textContent = shown + " / " + total + " " + this.noun;
        this.empty.hidden = shown > 0;
        this.list.hidden = shown === 0;
        const selectedPos = this.filtered.findIndex((entry) => entry.selected);
        this.setActive(selectedPos >= 0 ? selectedPos : (shown ? 0 : -1));
    };

    SearchableSelect.prototype.setActive = function (index) {
        this.activeIndex = index;
        Array.from(this.list.children).forEach((item, itemIndex) => {
            item.classList.toggle(ACTIVE_CLASS, itemIndex === index);
            if (itemIndex === index) {
                item.scrollIntoView({block: "nearest"});
            }
        });
    };

    SearchableSelect.prototype.onSearchKey = function (event) {
        if (event.key === "Escape") {
            event.preventDefault();
            this.close();
            this.trigger.focus();
            return;
        }
        if (event.key === "ArrowDown") {
            event.preventDefault();
            this.setActive(Math.min(this.filtered.length - 1, this.activeIndex + 1));
            return;
        }
        if (event.key === "ArrowUp") {
            event.preventDefault();
            this.setActive(Math.max(0, this.activeIndex - 1));
            return;
        }
        if (event.key === "Enter") {
            event.preventDefault();
            const entry = this.filtered[this.activeIndex];
            if (entry && !entry.disabled) {
                this.choose(entry.value);
            }
        }
    };

    SearchableSelect.prototype.choose = function (value) {
        const option = Array.from(this.select.options).find((item) => item.value === value);
        if (option && option.disabled && value) {
            return;
        }
        this.select.value = value;
        this.select.dispatchEvent(new Event("change", {bubbles: true}));
        this.select.dispatchEvent(new Event("input", {bubbles: true}));
        this.syncTrigger();
        this.close();
        this.trigger.focus();
    };

    function enhance(root) {
        const scope = root && root.querySelectorAll ? root : document;
        const selects = [];
        if (root instanceof HTMLSelectElement) {
            selects.push(root);
        }
        scope.querySelectorAll("select").forEach((select) => selects.push(select));
        selects.forEach((select) => {
            if (shouldSkip(select)) {
                return;
            }
            instances.set(select, new SearchableSelect(select));
        });
    }

    document.addEventListener("mousedown", function (event) {
        if (!openInstance) {
            return;
        }
        if (!openInstance.root.contains(event.target)) {
            openInstance.close();
        }
    });

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && openInstance) {
            openInstance.close();
        }
    });

    window.erpSearchableSelect = {
        enhance: enhance
    };

    function start() {
        enhance(document);
        document.body.addEventListener("htmx:afterSettle", function (event) {
            enhance(event.detail && event.detail.elt ? event.detail.elt : document);
        });
        new MutationObserver(function (mutations) {
            mutations.forEach(function (mutation) {
                mutation.addedNodes.forEach(function (node) {
                    if (node.nodeType !== 1) {
                        return;
                    }
                    enhance(node);
                });
            });
        }).observe(document.body, {childList: true, subtree: true});
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", start);
    } else {
        start();
    }
})();
