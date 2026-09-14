const PAGE_HELP_FOCUSABLE = 'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])';

function createPageHelpModal() {
    return {
        open: false,
        hasHelp: false,
        previousFocus: null,
        init() {
            const source = document.getElementById('page-help-source');
            const items = source ? source.querySelectorAll('[data-page-help-item]') : [];
            this.hasHelp = items.length > 0;
            if (this.$refs.helpList && source) {
                this.$refs.helpList.innerHTML = source.innerHTML;
            }
            this.$nextTick(() => {
                if (window.lucide) {
                    window.lucide.createIcons();
                }
            });
        },
        openHelp() {
            if (!this.hasHelp) {
                return;
            }
            this.previousFocus = document.activeElement;
            this.open = true;
            document.body.classList.add('page-help-modal-open');
            this.$nextTick(() => {
                if (window.lucide) {
                    window.lucide.createIcons();
                }
                const dialog = this.$refs.helpDialog;
                if (dialog) {
                    dialog.focus();
                }
            });
        },
        closeHelp() {
            if (!this.open) {
                return;
            }
            this.open = false;
            document.body.classList.remove('page-help-modal-open');
            const restore = this.previousFocus;
            this.previousFocus = null;
            if (restore && typeof restore.focus === 'function' && document.contains(restore)) {
                restore.focus();
            }
        },
        onKeydown(event) {
            if (!this.open) {
                return;
            }
            if (event.key === 'Escape') {
                event.preventDefault();
                this.closeHelp();
                return;
            }
            if (event.key === 'Tab') {
                this.keepFocusInside(event);
            }
        },
        keepFocusInside(event) {
            const dialog = this.$refs.helpDialog;
            if (!dialog) {
                return;
            }
            const nodes = Array.from(dialog.querySelectorAll(PAGE_HELP_FOCUSABLE))
                .filter((el) => !el.hasAttribute('disabled') && el.offsetParent !== null);
            if (nodes.length === 0) {
                event.preventDefault();
                dialog.focus();
                return;
            }
            const first = nodes[0];
            const last = nodes[nodes.length - 1];
            if (event.shiftKey && document.activeElement === first) {
                event.preventDefault();
                last.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
                event.preventDefault();
                first.focus();
            }
        }
    };
}

document.addEventListener('alpine:init', () => {
    Alpine.data('pageHelpModal', createPageHelpModal);
});
