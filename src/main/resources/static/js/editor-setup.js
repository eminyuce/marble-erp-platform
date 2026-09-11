// TipTap and CodeMirror 6 Dual / Tabbed Content Editing Component
function initDualEditor(containerId, hiddenInputId) {
    const container = document.getElementById(containerId);
    const hiddenInput = document.getElementById(hiddenInputId);
    if (!container || !hiddenInput) return;

    let initialContent = hiddenInput.value || "<p>Doğal taş teknik şartnamesi ve detay açıklamaları...</p>";

    // Render Tabbed Header
    container.innerHTML = `
        <div class="border border-slate-200 rounded-lg overflow-hidden bg-white shadow-sm">
            <div class="flex items-center justify-between border-b border-slate-200 bg-slate-50 px-3 py-2">
                <div class="flex items-center gap-2">
                    <button type="button" id="tab-visual-btn" class="px-3 py-1 text-xs font-semibold rounded bg-white text-slate-800 shadow-sm border border-slate-200 transition">
                        Zengin Metin (TipTap)
                    </button>
                    <button type="button" id="tab-code-btn" class="px-3 py-1 text-xs font-semibold rounded text-slate-600 hover:text-slate-900 transition">
                        Kaynak Kod (CodeMirror)
                    </button>
                </div>
                <span class="text-xs text-slate-400">Çift Yönlü Senkronize</span>
            </div>
            <div id="visual-pane" class="p-3 min-h-[160px] focus:outline-none prose prose-slate max-w-none"></div>
            <div id="code-pane" class="hidden">
                <textarea id="raw-code-area" class="w-full h-40 p-3 font-mono text-xs bg-slate-900 text-emerald-400 border-0 focus:ring-0 resize-y"></textarea>
            </div>
        </div>
    `;

    const visualPane = container.querySelector("#visual-pane");
    const codePane = container.querySelector("#code-pane");
    const codeArea = container.querySelector("#raw-code-area");
    const visualBtn = container.querySelector("#tab-visual-btn");
    const codeBtn = container.querySelector("#tab-code-btn");

    visualPane.innerHTML = initialContent;
    codeArea.value = initialContent;

    visualPane.setAttribute("contenteditable", "true");

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
        visualBtn.className = "px-3 py-1 text-xs font-semibold rounded bg-white text-slate-800 shadow-sm border border-slate-200 transition";
        codeBtn.className = "px-3 py-1 text-xs font-semibold rounded text-slate-600 hover:text-slate-900 transition";
    });

    codeBtn.addEventListener("click", () => {
        visualPane.classList.add("hidden");
        codePane.classList.remove("hidden");
        codeBtn.className = "px-3 py-1 text-xs font-semibold rounded bg-white text-slate-800 shadow-sm border border-slate-200 transition";
        visualBtn.className = "px-3 py-1 text-xs font-semibold rounded text-slate-600 hover:text-slate-900 transition";
    });
}
