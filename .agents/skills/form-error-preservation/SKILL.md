---
name: form-error-preservation
description: >-
  Keeps submitted form values when a business rule or validation error is shown in Özerler Marble ERP.
  Use when adding or changing forms, controllers, IllegalArgumentException, RejectedInputException,
  validation messages, errorMessage, or any submit that can fail for the user.
---

# Form Error Preservation

A rejected submit stays on the form. The user fixes the wrong value. They do not type the form again.

## Rules

1. Publish only the notice types that apply, including combinations: `successMessage` → Tamamlandı, `errorMessage` → İşlem tamamlanamadı, `warningMessage` or `invalidFields` → Kontrol edin. Empty types are omitted. Each published notice leaves after the seconds in `ui.notice.dismiss_seconds` (Sistem Ayarları, default 100). Never send `IllegalArgumentException` or `RejectedInputException` to the developer error page.
2. Keep every posted value except passwords, CSRF (`_csrf`), and file inputs. `UserFacingExceptionHandler` and `FormDraftInterceptor` copy the request into `preservedForm`. The layout script writes those values back.
3. When one input is wrong, name it. Pass the HTML `name` to `RejectedInputException` so that input gets `is-invalid`, a hint, and focus.
4. The sentence must use the same words as the labels on that screen (Turkish in `messages_tr.properties`).

## Throw a business rule

Let it propagate. Do not catch it and return an empty form.

```java
throw new RejectedInputException(
        MessageUtils.getMessage("error.factory.process.st_requires_strip"),
        "processType");
```

- The message is the full sentence the user reads.
- Each extra argument is an input `name`, not a Java field or a column.
- Omit names when no single input is known. The notice still shows and the other values stay.

`IllegalArgumentException` is still shown to the user and still keeps the draft. It does not highlight an input. Prefer `RejectedInputException` when the input is known.

## Controller catch

If a method already catches and re-renders the same view, set `errorMessage` and, when known, `invalidFields`:

```java
} catch (RejectedInputException ex) {
    model.addAttribute("errorMessage", ex.getMessage());
    model.addAttribute("invalidFields", ex.fieldNames());
    populateForm(model, locale);
    return "erp/example/form";
}
```

Do not `redirect:` on failure. A redirect without `preservedForm` drops the draft. The interceptor fills `preservedForm` only when `errorMessage` or `formErrors` is already set.

Do not clear the model and do not render a blank create page after a failed save.

## Validation

Bean messages and manual checks use the same notice. Field errors also add `is-invalid` on that input (`erp-form-input`, `erp-form-select`, `erp-form-textarea`, or the script for `invalidFields`).

## Do not

- Put the only explanation in a stack trace, status code, or exception class name.
- Ask the user to re-enter values that were accepted.
- Store passwords or CSRF in `preservedForm`.
- Highlight a name that is not the input's `name` attribute.
