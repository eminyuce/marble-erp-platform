---
name: erp-page-crud-protocol
description: >-
  Strict policy requiring full-page navigation for all Create and Edit operations across
  Özerler Marble ERP. Strictly prohibits modal popup dialogs for editing or creating entities
  unless explicitly requested. Mandates standard form routes, templates, and datagrid action links.
---

# ERP Full-Page CRUD Protocol (No-Modal Policy)

## 1. The Core Rule: Zero Modal Popups for Create & Edit

In **Özerler Marble ERP**, modal dialogs/popups (`<dialog>`, Alpine modals, popup forms) must **NEVER** be used for creating or editing entities.

### Strict Requirements:
- **Every Create action** MUST have a dedicated full page: `GET /<module>/create`
- **Every Edit action** MUST have a dedicated full page: `GET /<module>/{id}/edit`
- **Toolbar "Yeni ..." buttons** MUST be anchor links (`<a>`), NEVER `<button onclick="openCreateModal()">`:
  ```html
  <a th:href="@{/<module>/create}" class="erp-btn-primary">
      <i data-lucide="plus"></i>
      <span>Yeni Kayıt</span>
  </a>
  ```
- **Datagrid action rows** MUST navigate directly to the edit page via `href`, NEVER trigger a modal:
  ```javascript
  {icon: 'edit-3', label: 'Düzenle', href: '/<module>/' + row.id + '/edit'}
  ```
- **Modals are ONLY permitted** for destructive confirmation prompts (e.g. `Silmek istediğinize emin misiniz?` delete confirmation dialog), or when the user explicitly and intentionally requests a modal.

---

## 2. Standard URL Routing Pattern

All modules must follow standard RESTful page conventions:

| Action | HTTP Method | URL Path | View Template |
|---|---|---|---|
| List / Grid | `GET` | `/<module>` | `erp/<module>/index.html` |
| Create Page | `GET` | `/<module>/create` | `erp/<module>/form.html` |
| Create Submit | `POST` | `/<module>/create` | Redirect to `/<module>` or `/<module>/{id}` |
| Edit Page | `GET` | `/<module>/{id}/edit` | `erp/<module>/form.html` |
| Edit Submit | `POST` | `/<module>/{id}/edit` | Redirect to `/<module>` or `/<module>/{id}` |
| Detail Page | `GET` | `/<module>/{id}` | `erp/<module>/detail.html` |
| Delete Submit | `POST` | `/<module>/{id}/delete` | Redirect to `/<module>` |

---

## 3. Template Architecture for `form.html`

Form pages must extend `layout/base` and utilize shared ERP form components:

```html
<!DOCTYPE html>
<html lang="tr" xmlns:th="http://www.thymeleaf.org" xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout/base}">
<head>
    <title th:text="${record != null ? 'Kaydı Düzenle' : 'Yeni Kayıt'} + ' | Özerler Mermer ERP'"></title>
</head>
<body>
<div layout:fragment="content" class="erp-form-page">

    <!-- Breadcrumb & Page Heading -->
    <div th:replace="~{fragments/form-page :: breadcrumb('Modül Adı', @{/<module>}, ${record != null ? 'Düzenle' : 'Yeni'})}"></div>
    <div th:replace="~{fragments/form-page :: heading(
        ${record != null ? 'Kaydı Düzenle' : 'Yeni Kayıt'},
        'Kayıt açıklaması ve kullanım rehberi.',
        ${record != null ? ('#' + record.code) : ''},
        @{/<module>})}"></div>

    <div class="erp-form-card">
        <form th:action="${record != null ? '/<module>/' + record.id + '/edit' : '/<module>/create'}" method="post" class="erp-form">
            <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>

            <div class="erp-form-body">
                <div th:if="${errorMessage}" class="erp-form-errors" role="alert" th:text="${errorMessage}">Hata</div>

                <section class="erp-form-section">
                    <div th:replace="~{fragments/form-page :: section('Bölüm Başlığı', 'Bölüm açıklaması')}"></div>
                    <div class="erp-form-grid">
                        <div class="erp-form-field">
                            <label class="erp-form-label" for="fieldName">Alan Adı <span class="req">*</span></label>
                            <input id="fieldName" type="text" name="fieldName" th:value="${record != null ? record.fieldName : ''}" required class="erp-form-input">
                        </div>
                    </div>
                </section>
            </div>

            <!-- Standard Bottom Actions (Save / Cancel) -->
            <div th:replace="~{fragments/form-page :: actions(@{/<module>})}"></div>
        </form>
    </div>
</div>
</body>
</html>
```

---

## 4. Backend Controller Pattern

Controllers must implement distinct `create` and `edit` GET mappings alongside form validation:

```java
@GetMapping("/create")
public String createForm(Model model) {
    model.addAttribute("record", null);
    populateFormLookups(model);
    return "erp/<module>/form";
}

@PostMapping("/create")
public String createSubmit(@Valid @ModelAttribute("form") FormDto form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        populateFormLookups(model);
        return "erp/<module>/form";
    }
    // save logic
    redirectAttributes.addFlashAttribute("successMessage", "Kayıt başarıyla oluşturuldu.");
    return "redirect:/<module>";
}

@GetMapping("/{id}/edit")
public String editForm(@PathVariable("id") Long id, Model model) {
    Entity entity = service.getById(id);
    model.addAttribute("record", entity);
    populateFormLookups(model);
    return "erp/<module>/form";
}

@PostMapping("/{id}/edit")
public String editSubmit(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("form") FormDto form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
        populateFormLookups(model);
        return "erp/<module>/form";
    }
    // update logic
    redirectAttributes.addFlashAttribute("successMessage", "Kayıt başarıyla güncellendi.");
    return "redirect:/<module>";
}
```

---

## 5. Pre-Task Checklist

Before submitting any code changes involving Create or Edit:
1. [ ] Are Create and Edit hosted on dedicated `/create` and `/{id}/edit` URLs?
2. [ ] Are toolbar buttons standard `<a>` tags with `href`?
3. [ ] Are datagrid actions direct page links without modal triggers?
4. [ ] Does the page use `layout/base`, `fragments/form-page`, and `.erp-form-page` classes?
5. [ ] Is CSRF token included on all form submissions?
6. [ ] Are validation errors and flash notifications properly surfaced?
