(function () {
    const root = document.querySelector("[data-osf-guide]");
    if (!root) {
        return;
    }

    const sections = Array.from(root.querySelectorAll("[data-osf-section]"));
    const tocLinks = Array.from(root.querySelectorAll("[data-osf-toc-link]"));
    const prevBtn = root.querySelector("[data-osf-prev]");
    const nextBtn = root.querySelector("[data-osf-next]");
    const progressBar = root.querySelector("[data-osf-progress-bar]");
    const progressLabels = root.querySelectorAll("[data-osf-progress-label]");
    const content = root.querySelector("[data-osf-book]");
    const total = sections.length;
    let page = 0;

    function sectionIndexFromHash() {
        const hash = (window.location.hash || "").replace("#", "");
        if (!hash) {
            return 0;
        }
        const index = sections.findIndex((section) => section.id === hash);
        return index >= 0 ? index : 0;
    }

    function refreshIcons() {
        if (window.lucide) {
            window.lucide.createIcons();
        }
    }

    function go(index, updateHash) {
        if (index < 0 || index >= total) {
            return;
        }
        const shouldAnimate = page !== index && content;
        page = index;
        if (shouldAnimate) {
            content.classList.add("is-turning");
            window.setTimeout(() => content.classList.remove("is-turning"), 280);
        }
        sections.forEach((section, i) => {
            const active = i === page;
            section.hidden = !active;
            section.classList.toggle("is-active", active);
        });
        tocLinks.forEach((link) => {
            const active = Number(link.getAttribute("data-osf-page")) === page;
            link.classList.toggle("is-active", active);
            if (active) {
                link.setAttribute("aria-current", "page");
            } else {
                link.setAttribute("aria-current", "false");
            }
        });
        if (prevBtn) {
            prevBtn.disabled = page === 0;
        }
        if (nextBtn) {
            nextBtn.disabled = page === total - 1;
        }
        const label = (page + 1) + " / " + total;
        progressLabels.forEach((el) => {
            el.textContent = label;
        });
        if (progressBar) {
            progressBar.value = Math.round(((page + 1) / total) * 100);
        }
        if (updateHash && sections[page] && sections[page].id) {
            history.replaceState(null, "", "#" + sections[page].id);
        }
        refreshIcons();
    }

    tocLinks.forEach((link) => {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            go(Number(link.getAttribute("data-osf-page")), true);
        });
    });
    if (prevBtn) {
        prevBtn.addEventListener("click", () => go(page - 1, true));
    }
    if (nextBtn) {
        nextBtn.addEventListener("click", () => go(page + 1, true));
    }
    window.addEventListener("keydown", (event) => {
        if (event.target && ["INPUT", "TEXTAREA", "SELECT"].includes(event.target.tagName)) {
            return;
        }
        if (event.key === "ArrowRight") {
            go(page + 1, true);
        }
        if (event.key === "ArrowLeft") {
            go(page - 1, true);
        }
    });

    go(sectionIndexFromHash(), false);
})();
