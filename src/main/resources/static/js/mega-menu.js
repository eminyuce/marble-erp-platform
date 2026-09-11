(function () {
    const menu = document.getElementById("adminMegaMenu");
    const overlay = document.getElementById("adminMegaMenuOverlay");
    const openBtn = document.getElementById("adminMegaMenuOpen");
    const closeBtn = document.getElementById("adminMegaMenuClose");
    const searchInput = document.getElementById("adminSidebarSearchInput");
    const searchClear = document.getElementById("adminSidebarSearchClear");
    const emptyState = document.getElementById("adminSidebarSearchEmpty");
    const pageLabel = document.getElementById("adminTopbarPage");

    if (!menu || !openBtn) {
        return;
    }

    function refreshIcons() {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    }

    function initialsFromName(name) {
        if (!name) {
            return "ERP";
        }
        const local = name.split("@")[0].replace(/[^a-zA-Z0-9çğıöşüÇĞİÖŞÜ]/g, " ").trim();
        const parts = local.split(/[.\s_-]+/).filter(Boolean);
        if (parts.length >= 2) {
            return (parts[0][0] + parts[1][0]).toUpperCase();
        }
        return local.slice(0, 2).toUpperCase() || "ERP";
    }

    function setAvatar() {
        const nameEl = document.getElementById("adminMegaMenuTitle");
        const avatar = document.getElementById("adminMegaAvatar");
        if (nameEl && avatar) {
            avatar.textContent = initialsFromName(nameEl.textContent.trim());
        }
    }

    function setPageTitle() {
        if (!pageLabel) {
            return;
        }
        const raw = (document.title || "").split("|")[0].split("•")[0].trim();
        pageLabel.textContent = raw || "Yönetim";
    }

    function markActiveItem() {
        const path = (window.location.pathname.replace(/\/+$/, "") || "/");
        menu.querySelectorAll(".admin-mega-item").forEach((item) => {
            const href = item.getAttribute("href") || "";
            let target = href;
            try {
                target = new URL(href, window.location.origin).pathname.replace(/\/+$/, "") || "/";
            } catch (e) {
                target = href.replace(/\/+$/, "") || "/";
            }
            const isActive = path === target;
            item.classList.toggle("active", isActive);
            if (isActive) {
                item.setAttribute("aria-current", "page");
            } else {
                item.removeAttribute("aria-current");
            }
        });
    }

    function openMenu() {
        menu.hidden = false;
        menu.setAttribute("aria-hidden", "false");
        if (overlay) {
            overlay.hidden = false;
        }
        document.body.classList.add("admin-mega-open", "admin-mega-backdrop");
        openBtn.setAttribute("aria-expanded", "true");
        const closeLabel = openBtn.getAttribute("data-label-close");
        if (closeLabel) {
            openBtn.setAttribute("title", closeLabel);
            const labelEl = openBtn.querySelector(".admin-menu-trigger-label");
            if (labelEl) {
                labelEl.textContent = closeLabel;
            }
        }
        requestAnimationFrame(() => {
            menu.classList.add("is-open");
        });
        if (searchInput) {
            searchInput.focus();
        }
        refreshIcons();
    }

    function closeMenu() {
        menu.classList.remove("is-open");
        document.body.classList.remove("admin-mega-open", "admin-mega-backdrop");
        openBtn.setAttribute("aria-expanded", "false");
        const openLabel = openBtn.getAttribute("data-label-open");
        if (openLabel) {
            openBtn.setAttribute("title", openLabel);
            const labelEl = openBtn.querySelector(".admin-menu-trigger-label");
            if (labelEl) {
                labelEl.textContent = openLabel;
            }
        }
        window.setTimeout(() => {
            if (!menu.classList.contains("is-open")) {
                menu.hidden = true;
                menu.setAttribute("aria-hidden", "true");
                if (overlay) {
                    overlay.hidden = true;
                }
            }
        }, 220);
        if (searchInput) {
            searchInput.value = "";
            filterMenu("");
        }
    }

    function toggleMenu() {
        if (menu.classList.contains("is-open")) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    function filterMenu(query) {
        const q = (query || "").trim().toLowerCase();
        if (searchClear) {
            searchClear.hidden = !q;
        }
        let visibleCount = 0;
        menu.querySelectorAll("[data-mega-group]").forEach((card) => {
            let cardVisible = 0;
            card.querySelectorAll(".admin-mega-item").forEach((item) => {
                const haystack = ((item.getAttribute("data-search") || "") + " " + (item.textContent || "")).toLowerCase();
                const match = !q || haystack.includes(q);
                item.hidden = !match;
                item.classList.toggle("search-highlight", Boolean(q && match));
                if (match) {
                    cardVisible += 1;
                    visibleCount += 1;
                }
            });
            card.hidden = cardVisible === 0;
            const countEl = card.querySelector(".admin-mega-count");
            if (countEl && q) {
                countEl.textContent = String(cardVisible);
            }
        });
        if (emptyState) {
            emptyState.hidden = visibleCount > 0;
        }
        refreshIcons();
    }

    openBtn.addEventListener("click", (event) => {
        event.preventDefault();
        toggleMenu();
    });

    if (closeBtn) {
        closeBtn.addEventListener("click", closeMenu);
    }
    if (overlay) {
        overlay.addEventListener("click", closeMenu);
    }

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && menu.classList.contains("is-open")) {
            closeMenu();
        }
    });

    if (searchInput) {
        searchInput.addEventListener("input", () => filterMenu(searchInput.value));
    }
    if (searchClear) {
        searchClear.addEventListener("click", () => {
            searchInput.value = "";
            filterMenu("");
            searchInput.focus();
        });
    }

    setAvatar();
    setPageTitle();
    markActiveItem();
    refreshIcons();
})();
