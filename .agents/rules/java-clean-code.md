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

Prefer `calculateProductionCost()` over `calcProdCost()`.

## Methods

* Keep methods small and focused.
* A method should perform **one logical operation**.
* Avoid deeply nested conditionals.
* Prefer guard clauses / early returns.
* Avoid methods with excessive parameters.
* Extract complex business logic into well-named methods or domain services.
* Avoid boolean parameters when they make a method call unclear.

## Classes

* Keep classes cohesive with a clear single responsibility.
* Avoid God classes and unrelated business logic.
* Keep dependencies explicit through constructors.
* Prefer composition over inheritance unless inheritance represents a genuine "is-a" relationship.

## Error Handling

* Never silently swallow exceptions.
* Do not use exceptions for normal control flow.
* Catch exceptions at the appropriate architectural boundary.
* Preserve the original exception as the cause when rethrowing.
* Use meaningful domain-specific exceptions when appropriate.
* Error messages must provide useful context.

## Null Handling

* Avoid unnecessary `null`.
* Use `Optional` where it improves API semantics, particularly for return values.
* Do not blindly use `Optional` for every field or method parameter.
* Validate required inputs at system boundaries.

## Collections and Streams

* Prefer readable loops or streams based on which is clearer.
* Do not use streams merely to make code shorter.
* Avoid complex multi-stage streams that are difficult to understand.
* Avoid unnecessary intermediate collections.
* Do not mutate shared collections unexpectedly.

## Immutability & Constants

* Prefer immutable objects whenever practical.
* Use `final` where appropriate.
* Replace unexplained magic numbers and strings with meaningful constants or domain concepts.

## Duplication

* Follow **DRY**, but do not create premature abstractions.
* Do not create generic utility classes simply to eliminate a few lines of duplication.

## Business Logic & Architecture

* Keep business rules explicit and easy to locate (e.g. production cost, scrap cost, inventory availability, project profitability).
* Controllers coordinate requests, do not put business logic in controllers.
* Respect layer separation: API / Controller $\rightarrow$ Service $\rightarrow$ Domain $\rightarrow$ Repository / Persistence $\rightarrow$ Infrastructure.

## Database Code

* Keep SQL readable.
* Avoid N+1 queries.
* Do not retrieve data that is not required.
* Keep transaction boundaries explicit.

## Logging & Comments

* Use meaningful structured logging (no sensitive data, no `System.out.println()`).
* Prefer self-explanatory code over redundant comments.

## Tests & Refactoring

* Every meaningful business behavior must be testable.
* Update existing tests, add edge cases and failure scenarios.
* When refactoring, preserve existing behavior, make small focused changes, run tests.

### Golden Rule

**Do not optimize for fewer lines of code. Optimize for clarity, correctness, maintainability, and explicit business intent.**
Every change should leave the codebase **cleaner than it was before**.
