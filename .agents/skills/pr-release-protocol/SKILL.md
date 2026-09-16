---
name: pr-release-protocol
description: >-
  Mandatory protocol and checklist before creating any Pull Request or Merge Request.
  Enforces asset version cache-busting (app.asset-version), Tailwind CSS recompilation,
  window object exports for shared functions, datagrid index and pagination standards,
  and automated test validation to prevent stale browser cache regressions in production.
---

# Pull Request & Release Protocol (Asset Versioning & Frontend Safety)

This protocol must be followed by every developer and AI agent **before submitting or merging any Pull Request (PR) or Merge Request (MR)** that touches frontend files, static assets, templates, or UI layouts.

---

## 1. The Core Problem & Context

Production servers apply long-term HTTP caching headers to static assets for performance:
```yaml
spring:
  web:
    resources:
      cache:
        cachecontrol:
          max-age: 365d
          cache-public: true
```

Because browsers cache JavaScript and CSS files for up to **365 days**, any change to a script (e.g. adding `initMultiFilePond` or modifying `app.js` or `app.css`) will **NOT** be downloaded by existing users unless the asset query parameter `?v=${assetVersion}` is bumped.

If `assetVersion` is not updated:
- The user's browser executes the **old cached file from yesterday/last week**.
- Newly introduced functions throw: `Uncaught ReferenceError: <functionName> is not defined`.
- Datagrids lose new columns (like the `#` row index) or pagination controls.
- Localhost works fine (no cache), while **production fails immediately**.

---

## 2. Mandatory Pre-PR Checklist

Before creating any Pull Request, verify every item in this checklist:

### Checklist Item 1: Bump `app.asset-version`
If any `.js`, `.css`, `.html`, or `frontend/` file is modified:
1. Update `app.asset-version` in `src/main/resources/application.yml`:
   ```yaml
   app:
     asset-version: "YYYYMMDD-<feature-slug>" # e.g. 20260916-multifile-grid
   ```
2. Update the default value in `src/main/java/com/ozerler/marble/config/GlobalModelAttributes.java`:
   ```java
   @Value("${app.asset-version:YYYYMMDD-<feature-slug>}")
   private String configuredAssetVersion;
   ```
3. Ensure all static asset links in templates use the cache-buster:
   - `<link rel="stylesheet" th:href="@{/css/app.css(v=${assetVersion})}">`
   - `<script th:src="@{/js/app.js(v=${assetVersion})}"></script>`
   - `<script th:src="@{/js/filepond-setup.js(v=${assetVersion})}"></script>`
   - `<script th:src="@{/js/blocks-grid.js(v=${assetVersion})}"></script>`

### Checklist Item 2: Recompile Tailwind CSS
If `frontend/src/input.css` or any template classes were modified:
1. Navigate to `frontend/`:
   ```bash
   cd frontend
   npm run build
   ```
2. Verify that `src/main/resources/static/css/app.css` is updated and staged in git.

### Checklist Item 3: Explicit `window` Object Exports
Any function or utility called across different script tags or inline template scripts MUST be explicitly bound to `window`:
```javascript
// At the bottom of the script file:
window.initFilePond = initFilePond;
window.initMultiFilePond = initMultiFilePond;
window.deleteAttachedFile = deleteAttachedFile;
window.erpIndexColumn = erpIndexColumn;
```
Never rely solely on top-level `function foo() {}` declarations when scripts might execute in different scopes or asynchronous contexts.

### Checklist Item 4: Inline Fallbacks in `layout/base.html`
For critical grid and helper functions, verify that an inline fallback exists inside `base.html` before `<th:block layout:fragment="scripts">`:
- `window.gridText`
- `window.gridNumber`
- `window.gridMoney`
- `window.gridArea`
- `window.erpStatusBadge`
- `window.erpGridDefaults`
- `window.erpResponsiveCollapseColumn`
- `window.erpIndexColumn`

This prevents inline page scripts from throwing `ReferenceError` if an external script is delayed or blocked.

### Checklist Item 5: Defensive Inline Template Script Calls
In template fragment scripts, guard new function calls:
```html
<script th:inline="javascript">
    if (typeof initMultiFilePond === 'function') {
        initMultiFilePond('#block-filepond', '#uploaded-files-container', 'BLOCK', blockId);
    } else if (typeof window.initMultiFilePond === 'function') {
        window.initMultiFilePond('#block-filepond', '#uploaded-files-container', 'BLOCK', blockId);
    }
</script>
```

### Checklist Item 6: Datagrid Responsive & Pagination Standards
1. **Row Index Column (`#`):** Must remain visible across viewports. Never hide with `display: none !important` on mobile. Use compact width (`36px`) instead.
2. **Page Counter:** Ensure `paginationCounter: "rows"` is enabled in `erpGridDefaults()` so Tabulator displays `"Gösterilen 1-25 / X kayıt"`.
3. **Responsive Collapse:** Ensure `erpResponsiveCollapseColumn()` is the first column with `responsive: 0`.

### Checklist Item 7: Automated Test Validation
Run the full test suite before opening the PR:
```bash
./mvnw clean test
```
Ensure all grid and layout regression tests pass:
- `TabulatorGridUndefinedTest`
- `ErpToolbarLayoutTest`

---

## 3. Automated Protocol Step-by-Step

When preparing a Pull Request:

```bash
# 1. Compile CSS
cd frontend && npm run build && cd ..

# 2. Verify git status and check modified frontend files
git status

# 3. If JS/CSS/templates changed, bump assetVersion in:
#    - src/main/resources/application.yml
#    - src/main/java/com/ozerler/marble/config/GlobalModelAttributes.java

# 4. Run Maven tests
./mvnw test

# 5. Review git diff
git diff --stat
```
