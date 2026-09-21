---
name: java-clean-code
description: >-
  Apply Clean Code principles consistently across the entire Java codebase.
  Use when writing, refactoring, reviewing, or designing Java classes, methods,
  architectural layers, error handling, tests, and database interactions.
---

# Java Clean Code Principles

Apply **Clean Code principles** consistently across the entire Java codebase.

## Core Requirements

* Write code that is **clear, readable, maintainable, testable, and easy to extend**.
* Prefer simple and explicit implementations over clever or overly complex solutions.
* Follow **SOLID principles** where they improve the design.
* Keep classes and methods focused on a **single responsibility**.
* Avoid unnecessary abstraction, inheritance, interfaces, wrappers, and design patterns.
* Do not introduce complexity unless there is a clear business or technical requirement.

## Naming

Use descriptive, intention-revealing names.

* Classes: nouns
* Methods: verbs/actions
* Boolean variables: `is`, `has`, `can`, `should`
* Collections: use plural or descriptive names
* Avoid abbreviations and meaningless names such as `data`, `obj`, `temp`, `manager`, `helper`, or `util` unless genuinely appropriate.

Prefer:

```java
calculateProductionCost()
```

over:

```java
calcProdCost()
```

## Methods

* Keep methods small and focused.
* A method should perform **one logical operation**.
* Avoid deeply nested conditionals.
* Prefer guard clauses / early returns.
* Avoid methods with excessive parameters.
* Extract complex business logic into well-named methods or domain services.
* Avoid boolean parameters when they make a method call unclear.

Prefer:

```java
if (!isValidOrder(order)) {
    return;
}

processOrder(order);
```

over deeply nested logic.

## Classes

* Keep classes cohesive.
* Each class should have a clear responsibility.
* Avoid God classes.
* Avoid classes containing unrelated business logic.
* Keep dependencies explicit through constructors.
* Prefer composition over inheritance unless inheritance represents a genuine "is-a" relationship.

## Error Handling

* Never silently swallow exceptions.
* Do not use exceptions for normal control flow.
* Catch exceptions at the appropriate architectural boundary.
* Preserve the original exception as the cause when rethrowing.
* Use meaningful domain-specific exceptions when appropriate.
* Error messages must provide useful context.

Avoid:

```java
catch (Exception e) {
    // ignore
}
```

## Null Handling & Null Type Safety

* Avoid unnecessary `null`.
* Use `Optional` where it improves API semantics, particularly for return values.
* Do not blindly use `Optional` for every field or method parameter.
* Validate required inputs at system boundaries.
* Make nullability expectations obvious.

### Lambda Expressions vs. Method References for Null Safety

Under Spring Boot 4.x / Spring Framework 7 / Java 24, annotation-based null analysis (JSpecify / Eclipse JDT) inspects receiver parameters (`this`) in unbound instance method references. When an instance method is passed as a method reference (e.g. `String::trim`, `BigDecimal::add`), the receiver parameter `this` produces unchecked conversion warnings:
`Null type safety: parameter 'this' provided via method descriptor ... needs unchecked conversion to conform to '@NonNull ...'`

**Strict Policy:**
* **Always prefer explicit lambdas** instead of unbound instance method references in `Stream.map()`, `Stream.reduce()`, `Stream.filter()`, `Stream.flatMap()`, `Optional.map()`, `Comparator.comparing()`, and AssertJ `extracting()`:
  * Use `s -> s.trim()` instead of `String::trim`
  * Use `(a, b) -> a.add(b)` instead of `BigDecimal::add`
  * Use `dto -> dto.getAmount()` instead of `Dto::getAmount`
  * Use `entity -> entity.getId()` instead of `Entity::getId`
  * Use `item -> item.getLineTotal()` instead of `PurchaseOrderItem::getLineTotal`
  * Use `type -> type.name()` instead of `Enum::name`
  * Use `user -> user.isEnabled()` instead of `User::isEnabled`
  * Use `list -> list.stream()` instead of `List::stream`
  * Use `task -> task.run()` instead of `Runnable::run`
* **Never leave unused imports** when converting method references to lambdas (e.g., remove `import ...Role;` if `Role` was only referenced in `Role::getName`).

## Modernization, Deprecations & Zero-Warning Standards

* **Zero Warnings Policy:** Every class must compile with **zero warnings** in both Maven and Eclipse JDT (no null safety warnings, no deprecation warnings, no unused field or import warnings).
* **Spring Boot 4.x Modernization:**
  * Use `org.springframework.boot.EnvironmentPostProcessor` — the legacy interface `org.springframework.boot.env.EnvironmentPostProcessor` is deprecated since version 4.0.0 and marked for removal in 4.2.0.
  * Register processors in `META-INF/spring/org.springframework.boot.EnvironmentPostProcessor.imports`.
* **Apache POI (`SXSSFWorkbook`):**
  * Do not call deprecated `workbook.dispose()`. Use `try-with-resources` (`try (workbook; ...)`) where `workbook.close()` automatically disposes temporary files.
* **AssertJ Assertions:**
  * Do not use deprecated `asList()` from `AbstractAssert`. Use `asInstanceOf(InstanceOfAssertFactories.LIST)` instead.
* **Unused Fields:**
  * Never leave dead or unused fields (such as unused service injections or mail senders) in Spring `@Service`, `@Component`, `@Controller`, or model classes. If a component uses dynamic runtime configuration (e.g. database-driven SMTP), remove unused injected fields.

## Collections and Streams

* Prefer readable loops or streams based on which is clearer.
* Do not use streams merely to make code shorter.
* Avoid complex multi-stage streams that are difficult to understand.
* Avoid unnecessary intermediate collections.
* Do not mutate shared collections unexpectedly.

## Immutability

Prefer immutable objects whenever practical.

* Use `final` where appropriate.
* Avoid mutable shared state.
* Prefer immutable DTOs/value objects where appropriate.
* Do not expose mutable internal collections directly.

## Constants

Replace unexplained magic numbers and strings with meaningful constants or domain concepts.

Avoid:

```java
if (retryCount > 3) {
```

Prefer:

```java
if (retryCount > MAX_RETRY_COUNT) {
```

## Duplication

Follow **DRY**, but do not create premature abstractions.

Before extracting duplicated code:

1. Determine whether the duplicated logic represents the same business concept.
2. If yes, extract a reusable abstraction.
3. If not, keep the implementations independent.

Do not create generic utility classes simply to eliminate a few lines of duplication.

## Business Logic

Keep business rules explicit and easy to locate.

For example, calculations such as:

* production cost
* scrap cost
* inventory availability
* project profitability
* material consumption
* shipment cost

should not be scattered throughout controllers, repositories, or unrelated services.

Controllers should coordinate requests, not contain business logic.

## Architecture

Respect the existing project architecture.

Use appropriate separation between:

* API / Controller
* Application / Service
* Domain
* Persistence / Repository
* Infrastructure

Do not move code between layers unless there is a concrete architectural reason.

Dependencies should point in a controlled direction and domain logic should not unnecessarily depend on infrastructure concerns.

## Database Code

* Keep SQL readable.
* Avoid N+1 queries.
* Do not retrieve data that is not required.
* Use appropriate indexes and query conditions.
* Avoid putting complex business logic inside repository classes.
* Keep transaction boundaries explicit.
* Never modify database schema or production data unless explicitly requested.

## Logging

* Use meaningful structured logging.
* Do not log passwords, tokens, credentials, personal data, or sensitive information.
* Avoid excessive debug/info logging.
* Include relevant identifiers and context when diagnosing failures.
* Do not use `System.out.println()` in production code.

## Comments

Prefer self-explanatory code over comments.

Do not add comments that simply restate the code.

Good comments explain:

* Why something exists
* Why a non-obvious decision was made
* Important business constraints
* External system limitations

## Tests

Every meaningful business behavior must be testable.

When changing code:

1. Identify existing tests.
2. Update affected tests.
3. Add tests for new behavior.
4. Include edge cases and failure scenarios.
5. Do not weaken or remove tests simply to make them pass.

Prefer testing observable behavior rather than implementation details.

## Refactoring Rules

When refactoring:

* Preserve existing behavior unless explicitly instructed otherwise.
* Make small, focused changes.
* Avoid unrelated refactoring.
* Do not change public APIs unnecessarily.
* Do not introduce new frameworks or dependencies without justification.
* Maintain backward compatibility where required.
* Run formatting, compilation, static analysis, and tests after changes.

## AI Coding Behavior

Before modifying code:

1. Inspect the existing implementation.
2. Understand the surrounding architecture.
3. Search for existing patterns and reusable components.
4. Identify dependencies and side effects.
5. Make the smallest clean change that solves the problem.

After modifying code:

1. Review the changed code for Clean Code violations.
2. Remove unnecessary complexity.
3. Check naming and method/class responsibilities.
4. Check error handling.
5. Check null handling.
6. Check duplication.
7. Check test coverage.
8. Run relevant tests and static analysis.
9. Report exactly what changed and any remaining risks.

### Golden Rule

**Do not optimize for fewer lines of code. Optimize for clarity, correctness, maintainability, and explicit business intent.**

Every change should leave the codebase **cleaner than it was before**.
