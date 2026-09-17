// HTML notes editor: visual contenteditable + HTML source, auto-bound to notes fields.
function isNotesFieldName(name) {
    return name === "notes" || name === "saleNotes" || /Notes$/.test(name || "");
}

function initDualEditor(containerId, hiddenInputId, options) {
    const container = typeof containerId === "string" ? document.getElementById(containerId) : containerId;
    const hiddenInput = typeof hiddenInputId === "string" ? document.getElementById(hiddenInputId) : hiddenInputId;
    if (!container || !hiddenInput || container.dataset.editorReady === "1") {
        return;
    }

    const compact = !!(options && options.compact);
    const initialContent = hiddenInput.value || "";
    const paneMin = compact ? "min-h-[96px]" : "min-h-[160px]";

    container.dataset.editorReady = "1";
    container.innerHTML = `
        <div class="border border-slate-200 rounded-lg overflow-hidden bg-white shadow-sm">
            <div class="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-3 py-2">
                <div class="flex items-center gap-2">
                    <button type="button" data-editor-tab="visual"
                            class="px-3 py-1 text-xs font-semibold rounded bg-white text-slate-800 shadow-sm border border-slate-200 transition">
                        Zengin Metin
                    </button>
                    <button type="button" data-editor-tab="code"
                            class="px-3 py-1 text-xs font-semibold rounded text-slate-600 hover:text-slate-900 transition">
                        Kaynak Kod
                    </button>
                </div>
                <span class="text-xs text-slate-400">HTML</span>
            </div>
            <div data-editor-pane="visual" class="p-3 ${paneMin} focus:outline-none prose prose-slate max-w-none"></div>
            <div data-editor-pane="code" class="hidden">
                <textarea data-editor-code class="w-full h-40 p-3 font-mono text-xs bg-slate-900 text-emerald-400 border-0 focus:ring-0 resize-y"></textarea>
            </div>
        </div>
    `;

    const visualPane = container.querySelector("[data-editor-pane='visual']");
    const codePane = container.querySelector("[data-editor-pane='code']");
    const codeArea = container.querySelector("[data-editor-code]");
    const visualBtn = container.querySelector("[data-editor-tab='visual']");
    const codeBtn = container.querySelector("[data-editor-tab='code']");

    visualPane.innerHTML = initialContent;
    codeArea.value = initialContent;
    visualPane.setAttribute("contenteditable", "true");

    const activeTabClass = "px-3 py-1 text-xs font-semibold rounded bg-white text-slate-800 shadow-sm border border-slate-200 transition";
    const idleTabClass = "px-3 py-1 text-xs font-semibold rounded text-slate-600 hover:text-slate-900 transition";

    function updateContent(fromCode) {
        if (fromCode) {
            visualPane.innerHTML = codeArea.value;
            hiddenInput.value = codeArea.value;
        } else {
            codeArea.value = visualPane.innerHTML;
            hiddenInput.value = visualPane.innerHTML;
        }
    }

    visualPane.addEventListener("input", () => updateContent(false));
    codeArea.addEventListener("input", () => updateContent(true));
    visualBtn.addEventListener("click", () => {
        visualPane.classList.remove("hidden");
        codePane.classList.add("hidden");
        visualBtn.className = activeTabClass;
        codeBtn.className = idleTabClass;
        updateContent(false);
    });
    codeBtn.addEventListener("click", () => {
        visualPane.classList.add("hidden");
        codePane.classList.remove("hidden");
        codeBtn.className = activeTabClass;
        visualBtn.className = idleTabClass;
        updateContent(false);
    });

    const form = hiddenInput.form;
    if (form) {
        form.addEventListener("submit", function () {
            updateContent(visualPane.classList.contains("hidden"));
        });
    }
}

function uniqueEditorId(base) {
    let id = base;
    let n = 1;
    while (document.getElementById(id)) {
        id = base + "-" + (++n);
    }
    return id;
}

function preparedEditorHost(field) {
    const prev = field.previousElementSibling;
    if (prev && (prev.hasAttribute("data-html-notes") || prev.querySelector("[data-editor-pane]"))) {
        return prev;
    }
    return null;
}

function toHiddenNotesInput(field) {
    if (field instanceof HTMLInputElement && field.type === "hidden") {
        return field;
    }
    const hidden = document.createElement("input");
    hidden.type = "hidden";
    hidden.name = field.name;
    hidden.value = field.value || "";
    hidden.id = field.id || uniqueEditorId((field.name || "notes") + "-html");
    field.replaceWith(hidden);
    return hidden;
}

function bindPreparedNotesEditors(root) {
    root.querySelectorAll("[data-html-notes]").forEach(function (container) {
        const hiddenId = container.getAttribute("data-hidden-id");
        const hidden = hiddenId ? document.getElementById(hiddenId) : container.nextElementSibling;
        if (!container.id) {
            container.id = uniqueEditorId((hidden && hidden.id ? hidden.id : "notes") + "-editor");
        }
        initDualEditor(container.id, hidden && hidden.id ? hidden.id : hidden);
    });
}

function wrapVisibleNotesFields(root) {
    const fields = root.querySelectorAll("textarea, input");
    fields.forEach(function (field) {
        if (!(field instanceof HTMLInputElement || field instanceof HTMLTextAreaElement)) {
            return;
        }
        if (!isNotesFieldName(field.name) || field.dataset.htmlNotesBound === "1") {
            return;
        }
        const host = preparedEditorHost(field);
        if (field.type === "hidden" && host) {
            field.dataset.htmlNotesBound = "1";
            if (!host.id) {
                host.id = uniqueEditorId(field.id + "-editor");
            }
            initDualEditor(host.id, field.id);
            return;
        }
        if (field.type === "hidden") {
            return;
        }
        const compact = field.tagName === "INPUT";
        const hidden = toHiddenNotesInput(field);
        hidden.dataset.htmlNotesBound = "1";
        const container = document.createElement("div");
        container.id = uniqueEditorId(hidden.id + "-editor");
        container.setAttribute("data-html-notes", "");
        container.setAttribute("data-hidden-id", hidden.id);
        hidden.parentNode.insertBefore(container, hidden);
        initDualEditor(container.id, hidden.id, {compact: compact});
    });
}

function enhanceHtmlNotes(root) {
    const scope = root && root.querySelectorAll ? root : document;
    bindPreparedNotesEditors(scope);
    wrapVisibleNotesFields(scope);
}

function startHtmlNotes() {
    enhanceHtmlNotes(document);
    document.body.addEventListener("htmx:afterSettle", function (event) {
        enhanceHtmlNotes(event.detail && event.detail.elt ? event.detail.elt : document);
    });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", startHtmlNotes);
} else {
    startHtmlNotes();
}

window.initDualEditor = initDualEditor;
window.enhanceHtmlNotes = enhanceHtmlNotes;
window.isNotesFieldName = isNotesFieldName;
