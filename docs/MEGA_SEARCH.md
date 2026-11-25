# Mega Search — Production Design & UI/UX Proposal

> **Status:** Proposal / RFC (no production code changed yet)
> **Codebase:** `marble-erp-platform` 1.0.13
> **Date:** 2026-11-25
> **Audience:** product, backend, frontend, QA, operations
> **Scope:** dashboard-wide search across modules, records, and relevant UI content

---

## 1. Executive summary

### 1.1 The problem in one paragraph

The application already has a global search, but it is a thin fan-out over 13 repositories using `LIKE '%term%'` queries. It is flat (no grouping), limited to five hits per type, has no typo tolerance, no highlighting, no filters, no pagination, no caching, and no permission filtering inside the result set. It is also mounted under `/admin/dashboard/api/search`, which is restricted to `ADMIN` and `EXECUTIVE`, while the search box is shown to every authenticated user. The result is a search box that looks global but is slow at scale, hard to extend, and silently unusable for most roles.

### 1.2 Recommendation

Use **PostgreSQL 16 full-text search + `pg_trgm`** as the primary engine, with a denormalized `search_document` projection table. Keep the source of truth in the existing tables. Index only what users need to find, with module-level visibility. Add a single-query search API, grouped results, typo tolerance, Turkish-aware normalization, highlighting, filters, caching, and a keyboard-first UI built with the existing Thymeleaf + Alpine.js + Tailwind stack.

Do **not** add Elasticsearch, OpenSearch, Meilisearch, Typesense, or Algolia for the first release. The application is a single-tenant ERP on PostgreSQL 16 with a modest expected search corpus. A separate search service would add a second data store, a sync pipeline, ACL duplication, backup/restore work, and operational cost without a proportional benefit. Revisit that decision only when the measurable thresholds in §9.6 are crossed.

### 1.3 What “Mega Search” means here

| Dimension | Today | Target |
|---|---|---|
| Scope | 13 entity types, title/code only | 20+ types across operations, sales, finance, definitions, admin, and help/pages |
| Matching | `LOWER(col) LIKE '%q%'` | Full-text + prefix + trigram fuzzy + Turkish normalization |
| Ranking | 100 / 80 / 60 / 40 / 10 in memory | Weighted field rank + similarity + exact/prefix + recency + type priority |
| Grouping | Flat list | Grouped by type with counts and “see all” |
| Permissions | Endpoint admin-only; results not filtered | Visibility groups enforced in SQL and at the controller |
| Speed | 13 parallel scans per keystroke | One indexed query, p95 < 200 ms for suggestions, < 300 ms for full page |
| UX | Flat dropdown, no keyboard support | Clean landing page, command palette, `Ctrl/Cmd+K`, type tabs with counts, exact-phrase search, grouped previews, recent/popular searches, staged loading animations with cancel, filters, mobile overlay |
| Freshness | Live queries, no index | Near-real-time index (target 5–30 s), nightly reconcile, full reindex command |

---

## 2. Current state audit

### 2.1 Existing pieces

| Area | File / class | What it does |
|---|---|---|
| API | `controller/admin/GlobalSearchController.java` | `GET /admin/dashboard/api/search?q=` returns `BackEndResponse` wrapping `GlobalSearchResponse` |
| Service | `service/GlobalSearchService.java` | Fans out to 13 repositories on a thread pool, merges hits, scores in memory |
| DTOs | `dto/GlobalSearchHit.java`, `dto/GlobalSearchResponse.java` | `type`, `typeLabel`, `title`, `subtitle`, `url`, `icon`, `score`; flat list |
| Executor | `config/AsyncConfig.java`, `common/Constants.java` | `searchTaskExecutor`: core 4, max 13, queue 32; per-type limit 5; min query length 2 |
| Top bar | `templates/layout/navbar.html` | Inline Alpine component; fetch to `/admin/dashboard/api/search`; flat dropdown |
| Dashboard | `templates/admin/dashboard.html` | Duplicate fetch logic and flat result list; reads `?q=` on load |
| Menu filter | `static/js/mega-menu.js` | Client-side filter of menu items using `data-search` attributes |
| Grid search | Various `*Controller` / `*Service` | Per-module Tabulator search with server-side `LIKE`; separate from global search |
| Tests | `GlobalSearchServiceTest`, `GlobalSearchControllerTest` | Mocked repository fan-out; no integration, performance, or E2E search tests |
| Labels | `messages.properties`, `messages_tr.properties` | `search.type.*` labels for 13 types |

### 2.2 What works and should be kept

- The **top-bar placement** is correct. Users already look there.
- The **visual language** (slate/blue, Lucide icons, rounded cards) fits the ERP.
- The **per-module grid search** is appropriate for filtering a list and should stay.
- The **mega-menu client-side filter** is useful for navigation and should stay, but its `data-search` keywords should also feed the new “Pages” search type.
- The **`search.type.*` message keys** and Turkish labels can be extended, not replaced.
- The **`GlobalSearchHit` shape** is a useful starting point, but the new API needs groups, highlights, counts, and metadata.

### 2.3 Gaps and risks

1. **Leading-wildcard scans.** All global search repository methods use `LOWER(column) LIKE LOWER(CONCAT('%', :query, '%'))`. Existing B-tree indexes on `block_code`, `slab_code`, `customer.company_name`, etc. cannot be used with a leading `%`. Each keystroke scans 13 tables.
2. **Thread-pool amplification.** 13 concurrent queries per keystroke against a production Hikari pool of 5 (`application-prod.yml`: `DB_POOL_MAX:5`) can exhaust connections under even light concurrent use.
3. **Admin-only endpoint, everyone-visible input.** `SecurityConfig` restricts `/admin/**` to `ADMIN` and `EXECUTIVE`. The navbar renders the search box for `isAuthenticated()`. Non-admin users get a broken search.
4. **Unsafe landing pages.** `GlobalSearchService.searchUsers` returns `/admin/users/{id}/edit`; `searchCustomers` returns `/sales`; `searchSuppliers` returns `/procurement`. Some of these pages are admin-only or not record-specific. A result the user cannot open is worse than no result.
5. **No permission model in the result set.** If the endpoint is moved out of `/admin`, user/settings/template results would leak unless visibility is enforced in SQL.
6. **No typo tolerance or Turkish normalization.** `LOWER()` on the database does not reliably fold `İ/ı/ş/ğ/ç/ö/ü`; users must type exact characters. `mermr`, `santiye`, and `blk 2026 001` do not find the expected records.
7. **No grouping or counts.** A block code and a project name with the same score are interleaved. Users cannot see “12 blocks, 9 slabs, 5 projects”.
8. **No highlighting.** Users cannot see why a result matched.
9. **No pagination.** Five per type is a hard cap; there is no “see all” or “load more”.
10. **No cache, no metrics, no rate limit.** Every keystroke hits the database; there is no visibility into zero-result rate, latency, or click-through.
11. **No sync/index layer.** Search queries live tables directly, so there is no way to precompute normalized text or rank consistently.
12. **Duplicate frontend logic.** `navbar.html` and `dashboard.html` each implement their own fetch/debounce/state, which will drift.
13. **No slow-search experience.** A request that takes 2–5 seconds shows the same empty state as a broken one. There is no progress indicator, no staged “still working” message, no cancel, and no distinction between slow, timed out, and empty.
14. **No exact-phrase control.** Multi-word search is always broad; there is no way to say “match this phrase exactly”. Conversely, there is no protection against raw Markdown or formatting artifacts appearing in result titles.

### 2.4 Reuse, extend, replace

| Component | Decision |
|---|---|
| `GlobalSearchService` fan-out | **Replace** with `SearchService` + `SearchQueryRepository` |
| 13 `searchBy*` repository methods used only by global search | **Replace** with `SearchSource` implementations; keep `UserRepository.searchActiveUsers` for the admin grid |
| `GlobalSearchController` | Keep as a **deprecated adapter** for one release, then remove |
| `GlobalSearchHit` / `GlobalSearchResponse` | Replace with richer `SearchHit` / `SearchResponse`; keep old DTOs during the adapter period |
| Top-bar markup/styles | **Extend**; extract a shared `fragments/global-search.html` and `static/js/global-search.js` |
| Dashboard search block | **Replace** with the shared fragment; the full page becomes `/search` |
| `mega-menu.js` filter | **Keep**; also seed the “Pages” search type from a page catalog |
| `messages.search.type.*` | **Extend** with new types and UI copy |
| Per-module Tabulator search | **Keep**; add a “Bu modülde ara” chip that sets the search type filter |
| `Constants.SEARCH_*` | Reuse `SEARCH_MIN_QUERY_LENGTH`; retire the fan-out executor after migration |

---

## 3. Goals, non-goals, success metrics

### 3.1 Goals

- One search box that finds **any record the user is allowed to open**.
- Results in **under 200–300 ms** at p95 for the expected corpus.
- **Forgiving input**: partial codes, missing Turkish characters, small typos, spaces vs hyphens.
- **Grouped, explainable results**: type counts, matched-term highlighting, status/date/amount context.
- **Permission-safe**: users never see a result they cannot open.
- **Extensible**: a new module adds one `SearchSource` bean, one enum constant, and one label — no controller/UI changes.
- **Accessible and mobile-friendly**: keyboard-first, screen-reader friendly, full-screen on mobile.
- **Operable**: index lag, query latency, zero-result rate, and click-through are measurable.

### 3.2 Non-goals for the first release

- Natural-language question answering (“geçen ay en çok satan müşteri kim?”). That is reporting, not search.
- Semantic/vector search over all records. Codes and names need lexical matching; vectors are a later addition for help/notes.
- Cross-tenant search. The application is currently single-tenant.
- Searching arbitrary file contents. File metadata can be indexed; document text extraction is a later phase with ACL inheritance.
- Replacing per-module grid search. The grid search remains the right tool for filtering a list.

### 3.3 Success metrics

| Metric | Target | How measured |
|---|---|---|
| Suggestion p95 | < 200 ms | Micrometer timer on `/api/search/suggest` |
| Full results p95 | < 300 ms | Micrometer timer on `/api/search` |
| Time to first feedback | < 100 ms | Client metric `search.time_to_first_feedback_ms` |
| Slow-search abandonment | < 10% of requests > 2.5 s | `search.abandoned_after_ms` |
| Zero-result rate | < 15% of queries | `search_query_log` |
| Click-through rate (CTR) | > 35% of searches | `search_query_log.clicked_*` |
| Type-tab usage | > 20% of result-page sessions | Tab click events |
| Exact-phrase usage | 5–15% of searches, zero-result rate not worse than broad | `phrase_search=true`, `zero_result` |
| Index lag | < 30 s (MVP), < 5 s (Phase 2) | `search_index_state.last_incremental_at` |
| Permission leaks | 0 | Security tests per role; SQL visibility filter |
| Search-related support tickets | Down 50% after 4 weeks | Support log |

---

## 4. Architecture and approach

### 4.1 Options compared

| Approach | Pros | Cons | When to choose |
|---|---|---|---|
| **PostgreSQL FTS + `pg_trgm`** | No new service; transactional consistency; backups already cover it; GIN indexes; trigram typo tolerance; SQL joins for permissions; team already knows Postgres | `ts_rank_cd` is not BM25; GIN index size; Turkish stemming needs care; heavy write volume needs tuning | **Recommended now.** Single-tenant ERP, < 5–10M searchable docs, near-real-time is acceptable |
| **OpenSearch / Elasticsearch** | Best relevance control, facets, highlighting, scale, per-document ACL | Heavy ops, JVM/cluster memory, sync pipeline, duplicate data, backup/restore, security hardening | When corpus > 5–10M docs, multi-tenant SaaS, or complex analytics/relevance tuning is required |
| **Meilisearch / Typesense** | Very fast, simple API, good typo tolerance, easy relevance | Extra service and sync; ACL/filtering must be modeled; Turkish stemming/tokenization less mature; data duplication | Small/medium product where search is the core feature and ops can own another service |
| **Algolia** | Best-in-class relevance and UI tooling, managed | Cost at scale, data residency, per-record ACL via secured API keys, vendor lock-in | If budget is not a constraint and data residency allows it |
| **In-app Lucene / Hibernate Search** | Tight JPA integration, mass indexing, entity listeners | Adds Lucene index files, version coupling with Hibernate 7 / Boot 4.2, reindex/ops complexity, duplicate storage | If you want automatic entity indexing without SQL, and accept a new index format |
| **Hybrid lexical + vector (`pgvector`)** | Semantic recall for notes/help; can rerank | Embedding cost/latency; Turkish model quality; not useful for codes; more complexity | Phase 3 for help/notes only, after lexical search is stable |

**Decision:** PostgreSQL FTS + `pg_trgm`, with a denormalized projection table. Add `pgvector` only for help/notes in a later phase if metrics justify it.

### 4.2 Why not an external engine now

- The app is a **single-tenant monolith** with PostgreSQL as the source of truth. Search results must reflect writes immediately enough that a newly created invoice or block is findable. A separate engine introduces a sync window and a second failure mode.
- **Permissions are already in Postgres.** The simplest correct ACL is a `visibility` column filtered in the same SQL query. An external engine would need the same ACL duplicated and kept in sync.
- **Operations are already busy.** Adding OpenSearch/Meilisearch means another container, memory budget, upgrade path, backup, monitoring, and network policy.
- **The corpus is small.** An ERP with tens of thousands to low millions of records is well within Postgres GIN capabilities.
- **Turkish support is solvable in Java.** Normalize Turkish characters before indexing and querying; do not depend on database locale or a Turkish stemmer for codes.

Revisit when: > 5–10M searchable documents, p95 > 300 ms after tuning, multi-tenant isolation requirements, or a need for advanced relevance/analytics that Postgres cannot express.

### 4.3 Target architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│ Browser (Thymeleaf + Alpine.js + Tailwind)                          │
│  top bar search  ──►  /api/search/suggest   (live, grouped, 3/type) │
│  Ctrl/Cmd+K      ──►  command palette                               │
│  /search?q=...   ──►  /api/search           (full page, filters)    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ JSON (authenticated, CSRF for POST)
┌───────────────────────────────▼─────────────────────────────────────┐
│ Spring Boot 4.2                                                     │
│  SearchApiController  ──►  SearchService                            │
│                              ├─ SearchNormalizer (Turkish fold)     │
│                              ├─ SearchQueryParser (quotes, hints)  │
│                              ├─ SearchPermissionPolicy (visibility) │
│                              ├─ SearchQueryRepository               │
│                              │    ├─ PostgresSearchQueryRepository  │
│                              │    └─ SimpleSearchQueryRepository    │
│                              ├─ SearchCache (Caffeine)              │
│                              └─ SearchMetrics (Micrometer)          │
│                                                                     │
│  SearchIndexService  ◄──  SearchSource implementations              │
│    ├─ scheduled incremental sync (updated_date watermark)           │
│    ├─ explicit delete hooks (hard deletes)                          │
│    ├─ nightly full reconcile                                        │
│    └─ admin full reindex                                            │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ JDBC / JPA
┌───────────────────────────────▼─────────────────────────────────────┐
│ PostgreSQL 16                                                       │
│  search_document (projection, GIN FTS + GIN trigram + btree filters)│
│  search_index_state (version, last sync)                            │
│  search_query_log (analytics, 90-day retention)                     │
│  source tables (blocks, slabs, projects, invoices, ...)             │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.4 Data model

The projection table is deliberately denormalized. It contains everything needed to render a result and filter it, without joining source tables at query time.

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE search_document (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         VARCHAR(64)  NOT NULL DEFAULT 'default',
    doc_type          VARCHAR(40)  NOT NULL,
    entity_id         BIGINT       NOT NULL,
    title             TEXT         NOT NULL,
    subtitle          TEXT,
    code              TEXT,
    body              TEXT,
    url               TEXT         NOT NULL,
    icon              VARCHAR(40),
    visibility        VARCHAR(24)  NOT NULL DEFAULT 'ALL',
    status            VARCHAR(40),
    business_unit     VARCHAR(24),
    entity_date       DATE,
    amount            NUMERIC(16, 2),
    source_updated_at TIMESTAMP    NOT NULL,
    indexed_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- display columns stay raw; norm columns are populated by the indexer in Java
    title_norm        TEXT         NOT NULL,
    subtitle_norm     TEXT,
    code_norm         TEXT,
    body_norm         TEXT,
    search_text       TEXT         NOT NULL,  -- concat of norm fields, used by trigram/phrase
    search_vector     TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', coalesce(title_norm, '') || ' zzsep ' || coalesce(code_norm, '') || ' zzsep '), 'A') ||
        setweight(to_tsvector('simple', coalesce(subtitle_norm, '') || ' zzsep '), 'B') ||
        setweight(to_tsvector('simple', coalesce(body_norm, '')), 'C')
    ) STORED,
    CONSTRAINT uq_search_document UNIQUE (tenant_id, doc_type, entity_id)
);

CREATE INDEX idx_search_document_vector
    ON search_document USING GIN (search_vector);
CREATE INDEX idx_search_document_trgm
    ON search_document USING GIN (search_text gin_trgm_ops);
CREATE INDEX idx_search_document_type
    ON search_document (tenant_id, doc_type, source_updated_at DESC);
CREATE INDEX idx_search_document_visibility
    ON search_document (tenant_id, visibility);
CREATE INDEX idx_search_document_status
    ON search_document (tenant_id, doc_type, status);
CREATE INDEX idx_search_document_date
    ON search_document (tenant_id, doc_type, entity_date DESC);
```

Notes:

- **Normalize in Java, not in SQL.** Every display field has a `*_norm` twin (`title_norm`, `subtitle_norm`, `code_norm`, `body_norm`). They contain lowercase, Turkish-diacritic-folded text: `ç→c`, `ğ→g`, `ı→i`, `İ→i`, `ö→o`, `ş→s`, `ü→u`. This avoids PostgreSQL locale surprises (`lower('İ')` depends on the database collation) and makes `santiye` match `Şantiye`. The raw columns stay untouched for display.
- **`search_text` is the concatenation of the normalized columns.** It powers trigram similarity, exact substring checks, and phrase highlighting. Do not build it from raw display text.
- **`search_vector` uses the normalized columns and the `simple` configuration**, not `turkish`. Codes, names, and proper nouns should not be stemmed. `simple` + prefix matching + trigram is more predictable for an ERP. If prose recall later proves insufficient, add a second `search_vector_tr` using the `turkish` config and OR the two vectors.
- **Why this matters for exact phrase search:** `phraseto_tsquery('simple', 'santiye')` will never match a raw `Şantiye` vector. Phrase search and Turkish folding both depend on the vector being generated from normalized text.
- **`search_vector` is not mapped in JPA.** It is a Postgres-only generated column. The JPA entity maps only portable columns, so H2 local/test (`ddl-auto: create-drop`) still works. Hibernate schema validation ignores the extra Postgres column.
- **`visibility` is a single group**, not a per-user ACL. The current application has module-level read access, not row-level ownership. This keeps the index small and the query fast. See §4.6.
- **`status`, `business_unit`, `entity_date`, `amount`** are explicit filter/display columns. Do not hide them in JSONB; explicit columns are indexable and portable.
- **`tenant_id` defaults to `'default'`** for future multi-tenancy. The current codebase has no tenant column; do not pretend otherwise.

### 4.5 Indexing and synchronization

**Target freshness:** near-real-time, not real-time. A 5–30 second lag is acceptable for an ERP search; a newly created record should appear before the user navigates away.

**MVP pipeline (least invasive):**

1. Each searchable module implements `SearchSource`:
   ```java
   public interface SearchSource {
       SearchDocumentType type();
       List<SearchDocument> fetchChanged(Instant since, int limit);
       Optional<SearchDocument> fetchById(Long id);
       List<SearchDocument> fetchAll(int offset, int limit);
   }
   ```
2. `SearchIndexScheduler` runs every 30 seconds (configurable) and calls `fetchChanged(lastSync, batchSize)` for each source. Sources query `updated_date > :since` (53 of 56 entities extend `AuditableEntity`).
3. `SearchIndexService` batch-upserts 500 documents at a time with `ON CONFLICT (tenant_id, doc_type, entity_id) DO UPDATE`.
4. Hard-delete services call `searchIndexService.enqueueDelete(type, id)` in the same transaction. The known hard-delete points are limited (projects, customers, suppliers, blocks, machines, cost centers, email templates, cost transactions, cari transactions, pallet items). Add the call in those services.
5. A nightly `SearchReconcileJob` compares source tables to `search_document` and removes orphans, then runs a full reindex if the drift exceeds a threshold.
6. An admin action `POST /admin/search/reindex` triggers a full rebuild with progress in `search_index_state`.

**Phase 2 pipeline (stronger guarantees):**

Add a `search_outbox` table and database triggers on the searchable tables:

```sql
CREATE TABLE search_outbox (
    id          BIGSERIAL PRIMARY KEY,
    doc_type    VARCHAR(40) NOT NULL,
    entity_id   BIGINT      NOT NULL,
    operation   VARCHAR(10) NOT NULL CHECK (operation IN ('UPSERT', 'DELETE')),
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    attempts    INT         NOT NULL DEFAULT 0,
    last_error  TEXT
);
CREATE INDEX idx_search_outbox_pending ON search_outbox (id) WHERE processed_at IS NULL;
```

A row-level `AFTER INSERT OR UPDATE OR DELETE` trigger enqueues one row per change. A worker drains the outbox in order, builds the document, and upserts/deletes. This catches writes from native SQL, imports, and future modules without touching every service. It is the right long-term design if near-real-time delete correctness matters.

**Why not only application events?** There is no domain-event infrastructure today, and many writes happen in services with native SQL. An application-only pipeline would miss paths. Start with the watermark poller + explicit delete hooks; add the outbox when the freshness target tightens.

**Deletes:** prefer hard delete from `search_document` (the source tables remain the audit trail). If soft delete is needed for analytics, add `deleted_at` and filter it in every query.

**Idempotency:** every write is an upsert; every delete is `DELETE ... WHERE tenant_id = ? AND doc_type = ? AND entity_id = ?`. Outbox rows are processed in `id` order; duplicate upserts are harmless.

### 4.6 Permissions and multi-tenancy

The application is **single-tenant** today. There is no `tenant_id` on business tables. The only scoping concepts are:

- **Roles:** `ADMIN`, `EXECUTIVE`, `FACTORY_MANAGER`, `QUARRY_CHIEF`, `SITE_ENGINEER`, `WORKSHOP_CHIEF`, `OPERATOR`, `FINANCE`, `SALES`, `QC`, `USER`.
- **Business units** on customers, suppliers, checks, collections, cost centers: `QUARRY`, `FACTORY`, `WORKSHOP`, `SITE`.
- **URL-level restrictions:** `/admin/**` is `ADMIN`/`EXECUTIVE`; `/reports` is `ADMIN`/`EXECUTIVE`/`FINANCE`/`FACTORY_MANAGER`; most operational screens are available to any authenticated user.

Use a small set of **visibility groups** that mirror actual read access:

| Visibility | Who can see it | Typical types |
|---|---|---|
| `ALL` | Any authenticated user | blocks, slabs, cut items, projects, sales orders, purchase orders, invoices, collections, production, work orders, pallets, stock items, machines, cost centers, quarries, customers, suppliers, help/pages |
| `ADMIN` | `ROLE_ADMIN` only | users, settings, definitions (if kept admin-only), deployment |
| `ADMIN_EXEC` | `ROLE_ADMIN`, `ROLE_EXECUTIVE` | email templates, application logs, system health |
| `REPORTS` | `ROLE_ADMIN`, `ROLE_EXECUTIVE`, `ROLE_FINANCE`, `ROLE_FACTORY_MANAGER` | report catalog/pages |

The query always includes:

```sql
AND d.tenant_id = :tenantId
AND d.visibility IN (:visibilities)
```

`SearchPermissionPolicy` resolves the current user’s groups from `Authentication`. Do not trust a client-supplied role or visibility parameter.

**Landing-page parity is part of permissions.** A result must open a page the same user can read. Today:

- `searchUsers` returns `/admin/users/{id}/edit`, which is `ADMIN` only.
- `searchCustomers` returns `/sales` (generic), while the customer edit page is admin-only.
- `searchSuppliers` returns `/procurement` (generic).

Fix this in the feature:

- Add read-only customer/supplier detail routes under `/accounts/customers/{id}` and `/accounts/suppliers/{id}` for operational roles, or route non-admin hits to a filtered list page (`/sales?search=<code>`, `/procurement?search=<code>`).
- Add `?focus=<id>` or `#row-<id>` support to list pages so a result lands on the right row.
- Prefer a real detail route for production orders, collections, and checks; the current generic `/production` and `/collections` links are weak landing pages.

**If the business wants financial data restricted**, tighten the controller read access first (`/costs`, `/expenses`, `/invoices`, `/collections`), then set the corresponding visibility group. Do not make search stricter than the pages; that creates “I can open it but cannot find it” inconsistency.

**Multi-tenancy later:** keep `tenant_id` in the table and query from day one. When the app becomes multi-tenant, populate it from a server-side `TenantContext`, add `tenant_id` to source tables, and include it in every unique index and cache key. Never accept `tenant_id` from the request.

### 4.7 Hybrid / vector search later

Do not use vectors for codes, names, or amounts. A block code `BLK-2026-001` and a customer name `ABC Mermer` need exact/prefix/fuzzy lexical matching.

If help content, long notes, or email templates need semantic recall:

- Add `pgvector` to PostgreSQL 16.
- Add `embedding vector(768)` to a separate `search_embedding` table (not the main projection).
- Embed only help pages, notes, and template descriptions; not codes.
- Hybrid score: `0.7 * lexical + 0.3 * cosine`, rerank the top 50 lexical results.
- Keep a hard fallback to lexical if the embedding service is unavailable.

This is a Phase 3 experiment, not a launch requirement.

---

## 5. Search scope

### 5.1 Tier 1 — launch scope

These are the records users search for every day. Each row lists the fields to index and the safe landing page.

| Type | Fields to index | Visibility | Landing page |
|---|---|---|---|
| `BLOCK` | `blockCode`, `stoneType`, `quarry.name`, `quarry.code`, `status`, current location, assigned customer | `ALL` | `/genealogy?code=<blockCode>` (existing) or `/blocks?search=<code>` |
| `SLAB` | `slabCode`, `block.blockCode`, `status`, dimensions, surface area, location, customer | `ALL` | `/passport/<slabCode>` or `/production/slabs?search=<code>` |
| `CUT_ITEM` | `itemCode`, `cutOrder.cutOrderNo`, `targetLocation`, dimensions, status | `ALL` | `/genealogy?code=<itemCode>` |
| `PROJECT` | `projectCode`, `name`, `customerName`, `status`, `deliveryDate`, location | `ALL` | `/projects/{id}` |
| `CUSTOMER` | `customerCode`, `companyName`, `contactPerson`, `phone`, `email`, `taxNumber`, `businessUnit` | `ALL` | `/accounts/customers/{id}` (new read-only) or `/sales?search=<code>` |
| `SUPPLIER` | `supplierCode`, `companyName`, `contactPerson`, `phone`, `email`, `taxNumber`, `businessUnit` | `ALL` | `/accounts/suppliers/{id}` (new read-only) or `/procurement?search=<code>` |
| `INVOICE` | `invoiceNo`, `partyName`, `customer.companyName`, `supplier.companyName`, `project.name`, `totalAmount`, `invoiceDate`, `status`, `department` | `ALL` | `/invoices/{id}` |
| `SALES_ORDER` | `orderNo`, `customer.companyName`, `orderDate`, `deliveryDate`, `totalAmount`, `status` | `ALL` | `/sales/{id}` |
| `PURCHASE_ORDER` | `poNumber`, `supplier.companyName`, `orderDate`, `expectedDelivery`, `totalAmount`, `status` | `ALL` | `/procurement/{id}` |
| `PRODUCTION_ORDER` | `orderNo`, `machineName`, `operatorName`, `block.blockCode`, `status`, `startTime` | `ALL` | `/production?search=<orderNo>` or a detail route |
| `OPERATION_WORK_ORDER` | `orderNo`, `customer.companyName`, `stoneType`, `department`, `status`, `dueDate`, dimensions | `ALL` | `/work-orders?search=<orderNo>` |
| `COLLECTION` | `documentNo`, `customer.companyName`, `amount`, `collectionDate`, `method`, `status` | `ALL` | `/collections?search=<documentNo>` |
| `CHECK` | `documentNo`, `customer.companyName`, `amount`, `dueDate`, `status`, `bankName` | `ALL` | `/collections/checks?search=<documentNo>` |
| `PALLET` | `palletCode`, `warehouseLocation`, `customer.companyName`, `project.name`, `status`, `grossWeightKg` | `ALL` | `/production/pallets?search=<palletCode>` |
| `STOCK_ITEM` | `itemCode`, `stoneType`, `description`, dimensions, `status`, location, customer | `ALL` | `/operations/factory/sized?search=<itemCode>` |
| `MACHINE` | `code`, `name`, `machineType`, `location`, `status` | `ALL` | `/admin/definitions/machines/{id}/edit` (admin) or a read-only machine list |
| `COST_CENTER` | `code`, `name`, `businessUnit`, `description` | `ALL` | `/costs` |
| `QUARRY` | `code`, `name`, `location` | `ALL` | `/blocks` or `/admin/definitions/quarries/{id}/edit` (admin) |
| `PAGE` | Menu/page title, `data-search` keywords, description, URL | based on target route | actual route |
| `HELP` | Help page title, plain-text body, page key | `ALL` | open help panel via `open-help` event |

### 5.2 Tier 2 — next

| Type | Fields | Visibility | Notes |
|---|---|---|---|
| `USER` | full name, username, email, roles | `ADMIN` | Admin user management only; do not expose emails to non-admins |
| `SETTING` | key, description, category | `ADMIN` | **Never index values** (SMTP passwords, keys, tokens) |
| `EMAIL_TEMPLATE` | template name, subject, category, description | `ADMIN_EXEC` | Do not index `bodyHtml` unless it is reviewed for secrets |
| `APPLICATION_LOG` | timestamp, level, logger, message, username | `ADMIN_EXEC` | Prefer a dedicated log search page; high volume, sensitive |
| `FILE` | original file name, MIME type, parent entity type | inherit parent | No content extraction until ACL inheritance is proven |
| `REPORT` | report name, description | `REPORTS` | Report catalog |
| `SITE_CONSUMPTION`, `SHIPMENT`, `INSTALLATION`, `SCRAP_LOG` | document no, project, date, quantity, reason | `ALL` | Operational depth; add after Tier 1 stabilizes |
| `COST_TRANSACTION`, `CARI_TRANSACTION` | description, document no, amount, date, customer | `ALL` or `REPORTS` | Decide based on read-access policy; financial data is sensitive |

### 5.3 Relevant UI content

- **Menu/pages:** the mega menu already has `data-search` keywords (e.g., “ocaklar ocak blok takibi üretim sahası stok tonaj”). Move these into a `SearchPageCatalog` (YAML or Java list) so they are indexed consistently and testable. Each entry has `title`, `keywords`, `description`, `url`, `visibility`.
- **Help pages:** `HelpContentCatalog` loads 28 HTML files from `classpath:help/*.html` at startup. Strip HTML, index the plain text, and return a result that opens the existing help panel via `window.dispatchEvent(new CustomEvent('open-help', { detail: { pageKey } }))`.
- **Form/report names:** add a small catalog for report names and key screen titles. Do not scrape Thymeleaf templates; that is brittle.
- **Do not index:** passwords, TOTP secrets, API keys, SMTP credentials, system setting values, full application logs by default, or file contents.

### 5.4 Extensibility contract

Adding a new module should require **no changes** to the search controller, query repository, or UI. It should require:

1. A new `SearchDocumentType` enum constant:
   ```java
   public enum SearchDocumentType {
       BLOCK("block", "search.type.block", "box", SearchVisibility.ALL, 10),
       SLAB("slab", "search.type.slab", "layers", SearchVisibility.ALL, 20),
       // ...
       public String code();
       public String messageKey();
       public String icon();
       public SearchVisibility defaultVisibility();
       public int priority();
   }
   ```
2. A `@Component` implementing `SearchSource` for the entity:
   ```java
   @Component
   @RequiredArgsConstructor
   public class BlockSearchSource implements SearchSource {
       private final BlockRepository blockRepository;
       private final SearchDocumentMapper mapper;

       @Override public SearchDocumentType type() { return SearchDocumentType.BLOCK; }

       @Override
       public List<SearchDocument> fetchChanged(Instant since, int limit) {
           return blockRepository.findByUpdatedDateAfter(since, PageRequest.of(0, limit))
                   .stream().map(mapper::fromBlock).toList();
       }
       // fetchById, fetchAll
   }
   ```
3. A new `search.type.<code>` entry in `messages.properties` and `messages_tr.properties`.
4. If the module has a hard-delete path, call `searchIndexService.enqueueDelete(...)`.
5. If the module needs a new filter, add an explicit column to `search_document` and a facet entry — not a JSON path.

`SearchSourceRegistry` collects all `SearchSource` beans. `SearchIndexScheduler` and `SearchReindexService` iterate the registry. The UI reads groups from the API, so new types appear automatically.

---

## 6. Ranking and relevance

### 6.1 Scoring model

The score is a weighted sum of five signals, all computed in SQL:

```
score =
    0.50 * text_rank        -- ts_rank_cd with field weights A/B/C
  + 0.20 * trigram_rank     -- similarity(search_text, normalized_query)
  + 0.15 * exact_rank       -- exact code/title, prefix code, prefix title
  + 0.10 * type_priority    -- configurable per type (blocks/projects above settings)
  + 0.05 * recency          -- 1 / (1 + age_days / 30)
```

Field weights inside `search_vector`:

- **A:** `title`, `code` (block code, slab code, invoice no, order no, customer code)
- **B:** `subtitle` (customer name, stone type, status, location)
- **C:** `body` (notes, description, help text)

`exact_rank`:

- `1.00` if `code_norm = :q` or `title_norm = :q`
- `0.80` if `code_norm LIKE :q || '%'`
- `0.60` if `title_norm LIKE :q || '%'`
- `0.00` otherwise

`type_priority` is a small integer normalized to `0..1` from `SearchDocumentType.priority()`. It ensures that a block code match outranks a help page that happens to contain the same word, without hiding the help page.

If the query contains a quoted phrase, the phrase becomes a hard filter and a sixth signal is added. The weights are renormalized as described in §6.2.1; do not mix the two formulas in code.

Do not overfit the weights before you have click data. Start with the values above, log queries/clicks, and tune monthly.

### 6.2 Typo tolerance and partial matching

| Input | Behavior |
|---|---|
| `BLK-2026-001` | Exact code match first; also normalized `blk 2026 001` |
| `blk 2026` | Prefix match on `code`; FTS prefix on tokens |
| `mermr` | Trigram similarity ≥ 0.30 finds `mermer` |
| `santiye` | Normalizer folds `ş` → `s`, finds `Şantiye` |
| `ahmet yilmaz` | Folds `ı` → `i`, matches `Ahmet Yılmaz` |
| `12345` | Exact/prefix on codes and document numbers |
| 1 character | No search; show recent/popular instead (existing minimum is 2) |
| 2 characters | Prefix/exact only; skip trigram (too short) |
| > 200 characters | Reject with a friendly validation message |

Implementation details:

- `SearchNormalizer.normalize()` lowercases with `Locale.forLanguageTag("tr")`, folds Turkish diacritics, replaces punctuation/hyphens with spaces, collapses whitespace, and trims.
- `SearchQueryParser` extracts quoted phrases before `SearchQueryBuilder` runs; quotes are syntax only when used in pairs. `SearchQueryBuilder` builds a safe `to_tsquery('simple', ...)` from unquoted tokens, appending `:*` to the last token for prefix search. It escapes backslashes, `&`, `|`, `!`, `(`, `)`, `:`, and `*`; quoted phrases go through `phraseto_tsquery`, not string concatenation. Never concatenate raw input into SQL.
- Use `websearch_to_tsquery('simple', :q)` for multi-word natural input; use the custom prefix builder for the last token. Fall back to `plainto_tsquery` if parsing fails.
- For trigram, set `SET LOCAL pg_trgm.similarity_threshold = 0.30` for title/code matches and `0.25` for body matches. Do not run trigram for queries shorter than 3 characters.
- For very short codes, `fuzzystrmatch.levenshtein_less_equal` is an optional fallback; it is not needed for the first release.

#### 6.2.1 Exact phrase search (`"..."`)

Users sometimes know the exact phrase they want: `"mavi mermer"`, `"Ahmet Yılmaz"`, `"BLK 2026 001"`. Without quotes, multi-word input is intentionally forgiving, so word order and small typos are ignored. Quotes let the user say: **match this phrase exactly, do not broaden it.**

This is the only query syntax we should expose. Do not add `AND`, `OR`, `-`, wildcards, or field prefixes; those are database concepts, not user concepts.

**Behavior**

| Input | Behavior |
|---|---|
| `"mavi mermer"` | Exact phrase, same word order; no synonyms, no typo tolerance |
| `mavi mermer` | Broad search; word order flexible, typos and synonyms allowed |
| `fatura "mavi mermer"` | Invoice type + exact phrase |
| `"mavi mermer" "ocak a"` | Both phrases must match (AND) |
| `"mermer` | Unclosed quote: treat the rest as plain text, show `Tırnağı kapatmak için " ekleyin.`; do not block the search |
| `""` | Ignore the empty phrase |
| `"a"` | Ignore phrases shorter than 2 characters; show the minimum-length helper |
| `“mavi mermer”`, `„mavi mermer“`, `«mavi mermer»` | Normalize typographic quotes to a phrase |
| `'Ahmet'` | Apostrophe is **not** a phrase delimiter; Turkish names like `Ahmet'in` must keep working |
| `"BLK-2026-001"` | Normalize punctuation to spaces, match as the phrase `blk 2026 001` |
| `"'; DROP TABLE"` | Treated as literal phrase text; bind parameters only, never string concatenation |

**Parsing**

`SearchQueryParser` runs before `SearchQueryBuilder` and returns:

```java
public record ParsedQuery(
        List<String> phrases,      // normalized, max 3
        List<String> terms,        // unquoted tokens
        Set<SearchDocumentType> typeHints,
        boolean hasUnclosedQuote
) { }
```

- Normalize smart quotes (`“ ” „ « »`) to `"` before parsing. Do not normalize `'`.
- Split on unescaped `"`; support `\"` as a literal quote inside a phrase if users need it.
- Normalize each phrase with the same `SearchNormalizer` used for indexing: Turkish fold, lowercase, punctuation to spaces, collapse whitespace.
- Cap at **3 phrases**; additional phrases are treated as plain terms and the UI shows a hint. This prevents pathological queries.
- If the quoted phrase is shorter than 2 characters after normalization, drop it and treat it as plain text.
- Keep the original text for display; keep the normalized phrases for SQL and highlighting.

**SQL and ranking**

Quoted phrases are a **hard filter**, not a soft boost:

```sql
-- one condition per phrase, ANDed together
search_vector @@ phraseto_tsquery('simple', :normalizedPhrase)
```

- `phraseto_tsquery('simple', ...)` preserves word order and adjacency. The `simple` configuration is deliberate: no Turkish stemming inside codes and proper nouns.
- Add an exact substring check on the normalized projection for ranking: `position(:phrase in title_norm) > 0`, and the same for `subtitle_norm`, `code_norm`, and `body_norm`. Do not run it only on the concatenated `search_text`; a phrase must not match across a field boundary. This also catches punctuation-normalized matches such as `BLK-2026-001` vs `blk 2026 001`.
- When building `search_vector`, insert a sentinel token between weighted fields (for example `zzsep`) so `phraseto_tsquery` cannot match the end of the title plus the start of the subtitle. The sentinel is internal and never shown or queried.
- Apply the phrase filter before ranking; do **not** let trigram or synonym expansion add documents that do not contain the phrase.
- Ranking: add a sixth signal, `phrase_rank`, when phrases are present. Renormalize the other weights so the total stays `1.00`:

```
score =
    0.40 * text_rank
  + 0.12 * trigram_rank
  + 0.12 * exact_rank
  + 0.20 * phrase_rank      -- 1.00 title/code, 0.60 subtitle/body
  + 0.10 * type_priority
  + 0.06 * recency
```

- `phrase_rank` is `1.00` when the phrase appears in `title_norm` or `code_norm`, `0.60` when it appears only in `subtitle_norm` or `body_norm`, and `0.00` otherwise (but such rows are already excluded by the hard filter).
- Multiple phrases: each contributes to the filter; `phrase_rank` uses the best field match across phrases.
- Do not expand synonyms inside quotes. Synonyms outside quotes still expand normally.
- Do not apply trigram typo tolerance inside quotes. If the exact phrase returns zero results, offer `Tırnakları kaldır ve daha geniş ara` instead of silently broadening.

**Highlighting**

- Highlight the **whole matched phrase** as one range, not each word separately.
- If the phrase occurs multiple times, highlight all occurrences in the title and the first occurrence in the snippet.
- If the match came only from normalization (e.g., `santiye` vs `Şantiye`), highlight the original characters in the display text using a normalized-offset map; if that mapping is too complex for the first release, highlight the whole title instead of showing a wrong range.

**UX details**

- Show a small `Tam ifade` badge inside or under the input as soon as a valid quoted phrase is detected: `Tam ifade: "mavi mermer" ✕`.
- On mobile, add a `"` button in the input toolbar that wraps the current selection (or inserts a pair of quotes). Double quotes are buried in mobile keyboards; the toggle makes the feature usable.
- In the suggestion dropdown, offer `"mavi mermer" için tam ifade araması` when the user has typed a multi-word phrase without quotes. Selecting it wraps the text in quotes. This is the main discoverability path.
- On the results page, show the interpretation: `Tam ifade arandı: "mavi mermer" · 12 sonuç`.
- Zero exact results: `"mavi mermer" için tam ifade bulunamadı.` with buttons `Tırnakları kaldır` and `Filtreleri temizle`.
- Do not style the quote characters inside the input with a rich text editor. A plain `<input>` plus an interpretation chip is simpler, accessible, and mobile-friendly.

**Analytics**

Log `phrase_search=true`, `phrase_count`, `phrase_length_bucket` (not the phrase text by default), and `zero_result=true`. This tells you whether users understand the feature and where exact phrases fail.

### 6.3 Synonyms

Turkish ERP users use different words for the same thing. Add a small, admin-editable synonym table:

```sql
CREATE TABLE search_synonym (
    id BIGSERIAL PRIMARY KEY,
    term VARCHAR(80) NOT NULL,
    expansion VARCHAR(80) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'tr',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (term, expansion, locale)
);
```

Seed examples:

| Term | Expansions |
|---|---|
| `müşteri` | `cari`, `firma`, `hesap` |
| `tedarikçi` | `supplier`, `satıcı` |
| `plaka` | `slab` |
| `şantiye` | `proje`, `saha` |
| `ocak` | `quarry`, `maden` |
| `sipariş` | `order` |
| `fatura` | `invoice` |
| `tahsilat` | `collection`, `ödeme` |
| `çek` | `check`, `senet` |
| `iş emri` | `work order`, `üretim emri` |
| `stok` | `stock`, `envanter` |
| `ebat` | `sized`, `ölçü` |
| `cila` | `polish`, `parlatma` |
| `kesim` | `cutting`, `kesme` |

Query expansion: for each token, look up active synonyms and add `OR` terms with a lower boost. Cap at 3 expansions per token to avoid noise. Log expanded queries in `search_query_log` for tuning. Do not use a Postgres synonym dictionary file; it requires a server restart and is harder to edit.

### 6.4 Filters and facets

Launch filters:

- **Type:** horizontal single-select tabs with counts (`Tümü`, top types, `Diğer ▾`). Multi-select type filtering is deferred; it adds complexity without a clear user need in the first release.
- **Status:** per type, from the explicit `status` column (`IN_STOCK`, `AVAILABLE`, `ISSUED`, `PAID`, `OVERDUE`, ...). Use the existing enum labels; never show raw enum names.
- **Date range:** `entity_date` (invoice date, order date, due date, production date).
- **Business unit:** `QUARRY`, `FACTORY`, `WORKSHOP`, `SITE`.
- **Amount range:** optional for invoices/orders; use `amount`.
- **Related party:** customer/supplier/project name; resolve to IDs in the UI, filter by `subtitle`/`body` or an explicit `party_id` column if needed. Exact phrases are not filters; they are query semantics and are handled by the parser.

Facets are computed with `GROUP BY doc_type` and, for the selected type, `GROUP BY status`. Cache facet counts for 60 seconds per `(normalized query, visibility, filters, index version)`.

Do not expose raw SQL or JSON-path filters to the client. Accept a small allowlist of filter keys and map each to a bound parameter.

### 6.5 Grouping and highlighting

- The API returns `groups[]`, one per type, ordered by `SearchDocumentType.priority()`.
- Each group has `type`, `label`, `icon`, `total`, and `items[]`.
- For the typeahead, return at most 3 items per group and at most 12 groups; include `total` so the UI can say “12 blok” and “Tümünü gör”.
- For the full page, return 20 items per page across groups, or 20 per group with “load more”. Prefer a flat cursor page grouped in the UI; this keeps pagination stable.
- The full results page shows type tabs from the same `facets`/group totals. `Tümü` renders the grouped list; a selected type renders a flat list.
- Highlighting:
  - Server returns `highlights` as character ranges per field: `{ "title": [[0, 3]], "subtitle": [[8, 14]] }`.
  - For exact phrases, return `phraseHighlights` for the whole phrase range; do not split it into per-word ranges.
  - The client renders `<mark>` around those ranges using `textContent`, never `innerHTML`. This prevents XSS.
  - If ranges are hard to compute for trigram matches, return a `highlightTerm` and let the client find the first case-insensitive occurrence. Still use `textContent`.
  - `ts_headline` can be used for help/body snippets, but escape the source text and wrap only the matched tokens. Structured ranges are safer.

---

## 7. UI/UX design

### 7.1 Principles for non-technical users

The search box is used by quarry chiefs, workshop staff, sales, finance, and executives. It must feel like a helpful assistant, not a database console.

1. **One box, one optional syntax.** No `AND`, `OR`, wildcards, or field prefixes. Double quotes are the only syntax, and only when used in pairs: `"mavi mermer"` means exact phrase. Everything else is treated as plain text; an unmatched quote never blocks the search.
2. **Forgiving.** Partial codes, missing Turkish characters, spaces instead of hyphens, and small typos all work.
3. **Explain the match.** Show why a result appeared: matched text highlighted, type label, status, customer/project context.
4. **Never dead-end.** No results means tips, recent searches, and a “search everywhere” action — not an empty box.
5. **Speak Turkish, not database.** “Blok”, “Plaka”, “Fatura”, “Müşteri”; never `BLOCK`, `INVOICE`, `entity`, `index`.
6. **Fast feedback.** Progress and skeleton feedback within 100 ms; results within 200–300 ms; no full-page spinner for a keystroke. Slow responses escalate copy and offer cancel (§7.17).
7. **Keyboard and touch.** `Ctrl/Cmd+K` for power users, large touch targets for the yard/tablet.
8. **Trust.** Show “Yalnızca erişebildiğiniz kayıtlar gösterilir” once, subtly, not on every result.

### 7.2 Entry points and shortcuts

| Entry point | Behavior |
|---|---|
| Top bar search input | Always visible on desktop; icon-only on narrow screens; opens the dropdown on focus/typing |
| `Ctrl+K` / `Cmd+K` | Opens the command palette from anywhere; focuses the input and selects existing text |
| `/` | Focuses the top bar search when the user is not already typing in an input, textarea, select, or contenteditable |
| `Esc` | Closes the dropdown/palette; returns focus to the input |
| `↑` / `↓` | Moves through results across groups |
| `Enter` | Opens the active result; if no result is active, goes to the full results page |
| `Tab` | Moves focus to the next control (filters, “see all”) |
| `?` | Optional: opens a small shortcuts help popover when the input is focused |

Do not hijack `Ctrl+K` inside a text editor or a code field. Check `event.target`.

### 7.3 Search input anatomy

Desktop (top bar):

```
┌──────────────────────────────────────────────────────────────────────┐
│  🔍  Blok, plaka, proje, fatura veya müşteri ara…          [⌘K]  ✕  │
└──────────────────────────────────────────────────────────────────────┘
   Kodun bir kısmını yazabilirsiniz. Örn: BLK-2026, 12345, Ahmet Yılmaz.
   [Bloklarda] [Plakalarda] [Faturalarda] [Müşterilerde]   Filtrele ▾
```

- **Placeholder:** `Blok, plaka, proje, fatura veya müşteri ara…`
- **Helper text:** `Kodun bir kısmını yazabilirsiniz. Örn: BLK-2026, 12345, Ahmet Yılmaz.`
- **Shortcut hint:** a subtle `⌘K` / `Ctrl K` badge inside the input on desktop; hidden on mobile.
- **Clear button:** `✕` appears when there is text; `aria-label="Aramayı temizle"`.
- **Exact phrase:** typing `"mavi mermer"` searches that phrase exactly. Show a `Tam ifade: "mavi mermer" ✕` chip under the input when a valid phrase is detected. On mobile, add a `"` button that wraps the selection or inserts a quote pair (§6.2.1).
- **Query interpretation chips:** show `Tam ifade: "mavi mermer"` and `Tür: Fatura` as removable chips only when the user used quotes or a type keyword. These chips explain the search; they are not filters.
- **Scope chips:** `Her yerde`, `Bloklarda`, `Plakalarda`, `Faturalarda`, `Müşterilerde`. Chips are single-select for simplicity; “Her yerde” resets. The full results page uses the same single-select behavior via type tabs; status/date/unit filters live in the sidebar or sheet.
- **Filter button:** `Filtrele` with a count badge when active (`Filtrele (2)`).
- **Search button:** the form still submits to `/search` for no-JS; with JS, `Enter` opens the full results page and typing shows live suggestions.

Do not put the old “info” tooltip with a long paragraph in the input. Replace it with a short helper line and examples; the long explanation belongs in the help panel.

**Clean landing state.** The third reference screenshot shows a calmer alternative to chips above the fold: a large pill input, one line of instruction, a `Ctrl + K` hint, and nothing else. For `/search` with an empty query, use that layout:

```
                 ┌──────────────────────────────────────────────┐
                 │  🔍  Blok, plaka, proje, fatura veya müşteri  │
                 │      ara…                          [Ara]     │
                 └──────────────────────────────────────────────┘
                    Aramaya başlamak için blok kodu, plaka, proje,
                    fatura veya müşteri yazın.
                    İpucu: Ctrl + K ile her sayfadan açabilirsiniz.

                    Son aramalar · Popüler aramalar · Hızlı erişim
```

- The placeholder carries examples, not a feature list. Keep it under ~70 characters on desktop; truncate gracefully on mobile.
- The `Ara` button is disabled until the query has at least 2 characters, and shows a disabled gray state while a request is running.
- The shortcut hint uses `<kbd>` styling, not plain text.
- Mention exact phrase once, subtly: `Tam ifade için "mavi mermer" yazın.` Do not add a permanent syntax legend or a row of operators.
- Recent, popular, and quick links appear below the instruction, not inside the input.
- Do not show type tabs, filters, or result counts before the first query; they add noise to an empty page.

### 7.4 Input behavior

| Event | Behavior |
|---|---|
| Focus with empty input | Show recent searches, popular searches, and quick links — not an empty dropdown |
| Type 1 character | Show helper “En az 2 karakter yazın” and recent/popular; do not query |
| Type 2+ characters | Debounce 200 ms; show skeleton; fetch `/api/search/suggest` |
| Continue typing | Abort the previous request with `AbortController`; ignore stale responses by sequence number |
| Type a double quote | Enter phrase mode; when the quote is closed, show `Tam ifade` chip and search exactly. Unclosed quote: hint `Tırnağı kapatmak için " ekleyin.` and keep searching broadly |
| Select “tam ifade olarak ara” | Wrap the typed text in quotes and submit the phrase query |
| Clear | Reset results, keep focus, show recent/popular |
| Submit (`Enter`) | Navigate to `/search?q=...&type=...` (full page) |
| Select a result | Save the query to recent searches; navigate; optionally POST a click event |
| Network error | Keep the last results visible; show a small inline retry, not a full-page error |
| 429 | Show “Çok sık arama yaptınız. Birkaç saniye bekleyin.” |
| Slow response (> 800 ms) | Keep the progress bar and previous results; escalate copy per §7.17.6; offer cancel after 2.5 s |
| Timeout (8 s) | Show “Arama zaman aşımına uğradı. Daha spesifik bir kelime deneyin.” |

Debounce 200 ms for typing; 0 ms for explicit submit. Do not debounce the `Enter` key.

### 7.5 Dropdown / command palette layout

Desktop dropdown (anchored under the top bar input, max width 720 px):

```
┌──────────────────────────────────────────────────────────────────────┐
│ Yaklaşık 37 sonuç · 0,04 sn                          [Tümünü gör →]   │
├──────────────────────────────────────────────────────────────────────┤
│  BLOCKLAR  12                                           Tümünü gör  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ 📦 BLK-2026-001                                  [Stokta]      │  │
│  │    Beyaz Mermer · Ocak A · 12,4 ton                            │  │
│  │    Blok · Son güncelleme 2 gün önce                            │  │
│  └────────────────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ 📦 BLK-2026-014                                  [Fabrikada]   │  │
│  │    Gri Mermer · Ocak B · 9,8 ton                               │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  PLAKALAR  9                                            Tümünü gör  │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │ ▤ SLB-2026-0451                                  [Stokta]      │  │
│  │    BLK-2026-001 · 2 cm · 280 × 160 cm · 4,48 m²                │  │
│  └────────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  PROJELER  5                                            Tümünü gör  │
│  ...                                                                 │
├──────────────────────────────────────────────────────────────────────┤
│  ↑↓ gez · Enter aç · Esc kapat              Yalnızca yetkinizdekiler │
└──────────────────────────────────────────────────────────────────────┘
```

Rules:

- Show **3 items per group** in the dropdown; the group header shows the true total and “Tümünü gör”.
- Maximum 4 groups in the dropdown; if there are more, show “Diğer türlerde 8 sonuç” at the bottom.
- The first result is visually focused; arrow keys move the focus, not the mouse.
- The whole card is clickable; a small “Aç” button appears on hover/focus for clarity.
- If the user typed multiple words without quotes, show a first-row action: `"mavi mermer" için tam ifade araması`. Selecting it wraps the query and searches exactly.
- If the query already contains a valid quoted phrase, show a small `Tam ifade` chip in the dropdown header; do not repeat it on every card.
- The footer shows keyboard hints and a subtle lock icon with “Yalnızca yetkinizdekiler”.
- On mobile, the dropdown becomes a full-screen overlay with a sticky input and a close button.

Empty query state:

```
Son aramalar                         Temizle
  🕘 BLK-2026-001
  🕘 vadesi geçen çek
  🕘 Ahmet Yılmaz

Popüler aramalar
  [Beyaz mermer] [Stoktaki plakalar] [Vadesi geçen çekler]

Hızlı erişim
  [Bloklar] [Plakalar] [Projeler] [Faturalar] [Müşteriler]
```

No-results state:

```
        🔍
  Sonuç bulunamadı
  “mermr” için kayıt bulamadık.

  Şunları deneyebilirsiniz:
  • Yazımı kontrol edin (mermer)
  • Daha kısa bir kelime yazın (merm)
  • Filtreleri temizleyin
  • Tüm türlerde arayın

  [Tüm türlerde ara]  [Filtreleri temizle]  [Son aramalar]
```

Loading state:

- A 2 px indeterminate progress bar under the input appears immediately (§7.17.2).
- Skeleton rows with a shimmer replace the list only when there are no previous results; otherwise keep the previous results visible and dim them slightly (`opacity-60`).
- Escalate the waiting copy at 800 ms and 2.5 s, and offer “Aramayı iptal et” (§7.17.6).
- Announce the final count to screen readers with `aria-live="polite"`; do not announce every keystroke.
- Full timing, animation, and anti-pattern rules: §7.17.

Error state:

```
  Arama şu anda yanıt vermiyor.
  İnternet bağlantınızı kontrol edin veya tekrar deneyin.
  [Tekrar dene]
```

### 7.6 Full results page (`/search`)

Layout:

```
┌──────────────────────────────────────────────────────────────────────┐
│  🔍  mermer                                              [Ara]  ✕   │
│  Yaklaşık 37 sonuç · 0,04 sn                  [En alakalı ▾]        │
│  [Tümü 37] [Blok 12] [Plaka 9] [Proje 5] [Fatura 4] [Müşteri 3] [Diğer 4 ▾]    │
├───────────────┬──────────────────────────────────────────────────────┤
│  DURUM        │  BLOCKLAR  12                       Tümünü gör      │
│  ☐ Stokta (8) │  ┌────────────────────────────────────────────────┐  │
│  ☐ Rezerve (2)│  │ 📦 BLK-2026-001                    [Stokta]    │  │
│               │  │    Beyaz Mermer · Ocak A · 12,4 ton            │  │
│  TARİH        │  │    Blok · 2 gün önce güncellendi                │  │
│  [Son 7 gün]  │  └────────────────────────────────────────────────┘  │
│  [Son 30 gün] │  ...                                                 │
│  [Özel aralık]│                                                      │
│               │  PLAKALAR  9                         Tümünü gör      │
│  BİRİM        │  ...                                                 │
│  ☐ Ocak       │                                                      │
│  ☐ Fabrika    │  [Daha fazla göster]                                 │
│  ☐ Atölye     │                                                      │
│  ☐ Şantiye    │                                                      │
│               │                                                      │
│  [Filtreleri  │                                                      │
│   temizle]    │                                                      │
└───────────────┴──────────────────────────────────────────────────────┘
```

- **Type tabs are the primary type filter.** `Tümü` shows grouped sections; selecting `Blok` shows a flat, relevance-sorted list of blocks. Tabs carry counts so users can see the shape of the result set before scrolling.
- **The left sidebar** keeps status, date, business unit, amount, and party filters. Type checkboxes are removed from the sidebar to avoid duplicating the tabs.
- **Tab overflow:** show at most 8 tabs; collapse the rest into `Diğer (n) ▾`. On narrow screens the rail scrolls horizontally with a fade at the edge, and the active tab is scrolled into view.
- **Tab behavior:** single-select. Counts reflect the current query and non-type filters; selecting a type does not recompute the other counts, so users can switch without the numbers jumping.
- **Summary bar:** `Yaklaşık 37 sonuç · 0,04 sn` sits under the input. Use `Yaklaşık` only when fuzzy matching or an estimate was used; otherwise show `37 sonuç`. Keep the duration in seconds with Turkish decimal comma (`0,04 sn`).
- **URL is the state:** `/search?q=mermer&type=BLOCK&status=IN_STOCK&date=30d&sort=relevance`. Back/forward works; the page is shareable/bookmarkable.
- **Sort:** `En alakalı` (default), `En yeni`, `Koda göre`. Do not expose a dozen sort options.
- **Pagination:** “Daha fazla göster” with a cursor; keep the scroll position. Optional infinite scroll with `IntersectionObserver`, but always keep a button for accessibility.
- **Initial render is server-side** (Thymeleaf) so the page works without JS and has a fast first paint. Alpine enhances it for live updates and “load more”.
- **No results:** the same no-results block as the dropdown, with a “Filtreleri temizle” action and a link to the unfiltered query. If the query used quotes, also offer `Tırnakları kaldır`.
- **Result count:** show total and type breakdown; if the result set is capped, say “İlk 100 sonuç gösteriliyor” and provide a narrower search hint.
- **Selected card:** the card being opened or keyboard-focused gets a brand ring and a very light blue background, not a heavy black border (§7.18).

### 7.7 Result card anatomy

```
┌──────────────────────────────────────────────────────────────────────┐
│  [icon]  BLOK                                                       │
│          BLK-2026-001                                     [Stokta]   │
│          Beyaz Mermer · Ocak A · 12,4 ton                            │
│          Son güncelleme 2 gün önce                                   │
└──────────────────────────────────────────────────────────────────────┘
```

- **Type eyebrow:** a small uppercase label above the title (`BLOK`, `PLAKA`, `FATURA`). Do not repeat the same type in a chip below; one type label is enough.
- **Icon:** Lucide icon per type (`box`, `layers`, `building-2`, `receipt`, `users`, `truck`, `factory`, `scissors`, `package`, `wallet`, `mountain`, `calculator`, `file-text`, `circle-help`).
- **Title:** the code or name the user recognizes. Highlight matched characters with a soft yellow `<mark>` (`bg-amber-100 text-slate-900 rounded px-0.5`). For exact phrase searches, highlight the whole phrase as one range.
- **Subtitle:** 1 line of context: stone type, customer, project, dimensions, amount. Truncate with ellipsis; never wrap to 3 lines in the dropdown.
- **Meta line:** status badge, relative time (“2 gün önce”), optional amount. The type is already in the eyebrow; do not repeat it.
- **Snippet:** only when it adds information that is not already in the title or subtitle. A card that repeats the same sentence three times looks broken.
- **Selected/focused state:** `ring-2 ring-[#2b6cb0] bg-blue-50/40`; not a 2 px black border. Black reads as an error or a modal, and it fights the status badge colors.
- **Markdown safety:** never render raw indexed Markdown. Strip `**`, `__`, backticks, and HTML from titles/snippets at index time, or render them as plain text with `textContent`. The reference screenshot shows literal `**` and emoji leaking into titles; that is the single most visible quality bug to avoid.
- **Status badge colors:**
  - Green: `Stokta`, `Aktif`, `Ödendi`, `Tamamlandı`
  - Amber: `Rezerve`, `Bekliyor`, `Üretimde`, `Vadesi yaklaşıyor`
  - Red: `Vadesi geçti`, `İptal`, `Hurda`
  - Slate: `Kapalı`, `Arşiv`, `Bilinmiyor`
- **Amount/date:** format with `tr-TR` (`12.345,67 TL`, `25.11.2026`). Do not show raw ISO dates.
- **Actions:** whole card is a link; a small “Aç” button on hover/focus. Optional “Kopyala” for codes (clipboard) in Phase 2.
- **Preview:** on desktop, hovering/focusing a card can show a right-side preview panel with 4–6 key fields and “Kaydı aç”. This is Phase 2; the card must be useful without it.

### 7.8 Stats and information display

#### End-user stats (always visible, subtle)

| Stat | Where | Example | Why |
|---|---|---|---|
| Total results | Dropdown header, results page header | `Yaklaşık 223 sonuç` or `37 sonuç` | Sets expectations; “Yaklaşık” is honest when fuzzy matching was used |
| Type breakdown | Type tabs, dropdown header | `Blok 12 · Plaka 9 · Proje 5` | Helps users narrow without reading every card |
| Query interpretation | Results header, only when syntax was used | `Tam ifade: "mavi mermer"` | Explains why results are narrower than a broad search |
| Search duration | Dropdown footer / results header, small text | `0,21 sn` | Communicates speed; optional for non-technical users |
| Live wait time | Under the input while pending, after 800 ms | `Aranıyor… 1,2 sn` | Proves the system is still working on slow queries |
| Index freshness | Results footer, small text | `Son güncelleme: 2 dk önce` | Explains why a just-created record may not appear yet |
| Active filters | Results header | `2 filtre aktif` | Prevents “why so few results?” confusion |
| Capped results | Results footer | `İlk 100 sonuç gösteriliyor` | Explains why “load more” stops |
| Permission note | Dropdown footer, once | `Yalnızca yetkinizdeki kayıtlar` | Builds trust without clutter |

Do not show raw milliseconds to every user by default. Show `0,04 sn` in the footer; if the product team prefers, hide it behind a “Arama ayrıntıları” tooltip.

**Count semantics.** Use `Yaklaşık` when trigram/fuzzy matching contributed results or when the count comes from an estimate; use a plain number when every result matched exactly. Do not show “Yaklaşık” for a simple exact/prefix search; it makes a precise result feel uncertain. If tabs show counts, the `Tümü` count must equal the sum of the visible type counts (or the UI must explain overlap). In the reference screenshot, `12 + 16 + 9 + 2 + 13 + 73 + 4 + 74 + 20 = 223`, which is exactly the `Tümü` count; that consistency is what makes the tabs trustworthy.

#### Admin stats (`/admin/search/analytics`, ADMIN only)

| Stat | Source | Use |
|---|---|---|
| Indexed documents by type | `search_document` group by `doc_type` | Coverage; detect missing modules |
| Index lag | `search_index_state.last_incremental_at` | Freshness SLO |
| Last full reindex | `search_index_state.last_full_reindex_at` | Operations |
| Query p50/p95/p99 | Micrometer / `search_query_log.took_ms` | Performance |
| Zero-result rate | `search_query_log` where `result_count = 0` | Content/synonym gaps |
| Top queries (7/30 days) | `search_query_log` | Popularity; quick links |
| Top zero-result queries | `search_query_log` | Add synonyms/content |
| Click-through rate by type | `clicked_doc_type` / searches | Ranking quality |
| Top clicked results | `clicked_doc_id` | “Did you mean” and quick links |
| Failed index jobs | `search_outbox`/job table | Reliability |
| Cache hit rate | Caffeine stats | Tuning |

The admin page should offer actions: **“Şimdi yeniden indeksle”**, **“Eşitle”**, **“Eşanlamlı ekle”**, and a download of the top zero-result queries.

### 7.9 States summary

| State | UI | Copy |
|---|---|---|
| Landing / empty query | Clean centered input + instruction + recent/popular/quick links (§7.18.6) | `Aramaya başlamak için blok kodu, plaka, proje veya fatura yazın.` |
| Idle / empty input (top bar) | Recent + popular + quick links | `Ne aramak istersiniz?` |
| Typing, < 2 chars | Helper, no query | `En az 2 karakter yazın.` |
| Loading (fast) | Progress bar + dimmed previous results; skeleton only if no previous results | `Aranıyor…` |
| Loading (full page, first query) | Centered scan animation, query echoed, stage copy, cancel (§7.18.4) | `"mavi mermer" aranıyor` |
| Loading (slow, > 800 ms) | Escalating inline copy + cancel action | `Biraz uzun sürüyor…` |
| Loading (very slow, > 2.5 s) | Same, with a clearer message and cancel | `Hâlâ arıyoruz, lütfen bekleyin.` |
| Results | Type tabs + grouped cards (Tümü) or flat list (selected type), counts, highlight | `Yaklaşık 37 sonuç · 0,04 sn` |
| Results with quotes | Interpretation chip + exact phrase results | `Tam ifade arandı: "mavi mermer"` |
| No results | Tips + actions | `Sonuç bulunamadı` |
| No exact phrase results | Phrase-specific recovery | `"mavi mermer" için tam ifade bulunamadı.` |
| Error | Inline retry | `Arama şu anda yanıt vermiyor.` |
| Rate limited | Inline warning | `Çok sık arama yaptınız. Birkaç saniye bekleyin.` |
| Timeout | Inline retry + narrower hint | `Arama zaman aşımına uğradı.` |
| Degraded | Subtle banner | `Şu anda temel arama kullanılıyor.` |
| Unauthorized direct link | Access denied page | `Bu kaydı görüntüleme yetkiniz yok.` |

### 7.10 Recent searches, popular searches, suggestions

- **Recent searches:** `localStorage` key `erp.search.recent.v1`; max 8; dedupe case-insensitively; newest first; clear-all button; skip queries shorter than 2 characters. Do not store sensitive-looking queries (e.g., “şifre”, “parola”, “token”) — filter them out.
- **Popular searches:** aggregated from `search_query_log` (last 30 days), admin-curated allowlist. Show 4–6 chips. Do not show a query that returns zero results.
- **Suggestions while typing:**
  - “Bunu mu demek istediniz?” for a likely typo (`mermr` → `mermer`), computed from trigram similarity against indexed titles and a curated term list.
  - “Şunlarda ara:” type chips when the query matches a type keyword (`fatura`, `blok`, `plaka`, `proje`).
  - “Tam ifade olarak ara: `"mavi mermer"`” when the user typed multiple words without quotes. This is the discovery path for exact phrase search.
  - “Son aramalar” when the input is empty.
- **Quote handling in suggestions:** if a quote is open, do not append type keywords inside the quoted text; show the closing-quote hint instead. If a phrase is already quoted, suggestions must stay within that phrase.
- **No-results recovery:** show the closest matching term, remove filters, and offer “Tüm türlerde ara”.

### 7.11 Filters in plain language

| Technical filter | UI label | Control |
|---|---|---|
| `doc_type` | `Tür` | Horizontal tabs with counts: `Tümü`, top types, `Diğer ▾` (single-select) |
| `status` | `Durum` | Checkbox chips with Turkish labels |
| `entity_date` | `Tarih` | `Son 7 gün`, `Son 30 gün`, `Bu yıl`, `Özel aralık` |
| `business_unit` | `Birim` | `Ocak`, `Fabrika`, `Atölye`, `Şantiye` |
| `amount` | `Tutar` | `Min` / `Max` inputs with TL suffix |
| party | `Müşteri / Tedarikçi` | Searchable select (existing `searchable-select.js`) |
| project | `Proje` | Searchable select |

- Filters apply immediately; no “Apply” button.
- Active filters appear as removable chips above the results: `Blok ✕`, `Stokta ✕`, `Son 30 gün ✕`.
- “Filtreleri temizle” resets all filters but keeps the query.
- Filter state is in the URL, so the page can be shared/bookmarked.
- Zero-count filters are disabled with a tooltip: `Bu filtreyle sonuç yok`.
- Type tabs are single-select and live above the results, not in the sidebar. This keeps the most common filter one click away and avoids duplicating the same control in two places.
- Tab counts reflect the current query and non-type filters. They do not change when a different type tab is selected, so the numbers stay stable while the user explores.
- If there are more than 8 types, the overflow menu shows `Diğer (n) ▾` with the remaining types and counts. On mobile, the tab rail scrolls horizontally; do not wrap it into a second row.

### 7.12 Accessibility

- Use the ARIA combobox pattern:
  - Input: `role="combobox"`, `aria-autocomplete="list"`, `aria-expanded`, `aria-controls="search-results"`, `aria-activedescendant="search-option-<id>"`.
  - Results: `role="listbox"`, each result `role="option"`, `aria-selected`.
  - Count: `aria-live="polite"` (not `assertive`).
- Type tabs use the ARIA tabs pattern: `role="tablist"`, `role="tab"`, `aria-selected`, `aria-controls`, and `role="tabpanel"`. Support `←`/`→`, `Home`, and `End`. The active tab is always visible in the scrollable rail.
- The quote/exact-phrase chip is a button with `aria-label="Tam ifade aramasını kaldır"`; the quote toggle on mobile has an accessible name, not just a `"` glyph.
- The search input has a real label (`<label class="sr-only">`) or `aria-label="Sitede ara"`; the placeholder is not a label.
- Visible focus ring on the input, chips, tabs, cards, and buttons (`focus-visible:ring-2 focus-visible:ring-[#2b6cb0]`).
- Keyboard: all actions reachable without a mouse; `Esc` closes; focus returns to the input.
- Color contrast: at least 4.5:1 for text; status badges use text + color, not color alone.
- Reduced motion: respect `prefers-reduced-motion`; disable scale/fade animations.
- Screen-reader labels for icon-only buttons (`Aramayı temizle`, `Filtreleri aç`, `Sonuçları kapat`).
- Do not use `innerHTML` for highlighted text; use text nodes and `<mark>`.

### 7.13 Mobile

- The top bar search becomes an icon; tapping it opens a full-screen overlay.
- Input is 16px to avoid iOS zoom; `inputmode="search"`, `enterkeyhint="search"`, `autocomplete="off"`.
- Sticky header with back/close, input, and filter button.
- Type tabs scroll horizontally under the sticky header; the active tab is scrolled into view. Do not wrap to a second row.
- Add a `"` button to the input toolbar for exact phrase search; wrapping the selection is easier than finding double quotes on a mobile keyboard.
- Results fill the screen; group headers are sticky.
- Filters open as a bottom sheet with a “Bitti” button and a “Temizle” link; filters apply immediately, so “Bitti” only closes the sheet.
- Touch targets at least 44×44 px; cards have 12–16px vertical padding.
- Recent/popular searches are scrollable; no hover-only actions.
- The full results page uses a single column; the sidebar becomes a filter sheet.

### 7.14 Turkish microcopy

| Context | Copy |
|---|---|
| Landing instruction | `Aramaya başlamak için blok kodu, plaka, proje, fatura veya müşteri yazın.` |
| Landing shortcut | `İpucu: Ctrl + K ile her sayfadan açabilirsiniz.` |
| Placeholder | `Blok, plaka, proje, fatura veya müşteri ara…` |
| Helper | `Kodun bir kısmını yazabilirsiniz. Örn: BLK-2026, 12345, Ahmet Yılmaz.` |
| Exact phrase hint | `Tam ifade için tırnak kullanın: "mavi mermer".` |
| Exact phrase chip | `Tam ifade: "mavi mermer"` |
| Remove phrase | `Tam ifade aramasını kaldır` |
| Unclosed quote | `Tırnağı kapatmak için " ekleyin.` |
| Phrase suggestion | `"mavi mermer" için tam ifade araması` |
| Phrase results | `Tam ifade arandı: "mavi mermer"` |
| Phrase no results | `"mavi mermer" için tam ifade bulunamadı.` |
| Remove quotes | `Tırnakları kaldır` |
| Result summary | `Yaklaşık 223 sonuç · 0,21 sn` |
| Tabs | `Tümü`, `Blok`, `Plaka`, `Fatura`, `Diğer` |
| Full-page loading | `"mavi mermer" aranıyor` |
| Stage copy | `Bloklar ve plakalar taranıyor…` |
| Stage copy | `Faturalar ve cari hesaplar kontrol ediliyor…` |
| Stage copy | `Sonuçlar hazırlanıyor…` |
| Min length | `En az 2 karakter yazın.` |
| Loading | `Aranıyor…` |
| Slow search (0.8 s+) | `Biraz uzun sürüyor…` |
| Very slow search (2.5 s+) | `Hâlâ arıyoruz, lütfen bekleyin.` |
| Almost done (6 s+) | `Neredeyse tamam…` |
| Cancel search | `Aramayı iptal et` |
| Partial results | `Sonuçlar geliyor…` |
| Still searching other types | `Diğerleri aranıyor…` |
| Results | `Yaklaşık 37 sonuç · 0,04 sn` |
| Group header | `Bloklar · 12` |
| See all | `Tümünü gör` |
| Load more | `Daha fazla göster` |
| No results title | `Sonuç bulunamadı` |
| No results body | `Yazımı kontrol edin, daha kısa bir kelime deneyin veya filtreleri temizleyin.` |
| Clear filters | `Filtreleri temizle` |
| Search everywhere | `Tüm türlerde ara` |
| Recent | `Son aramalar` |
| Clear recent | `Temizle` |
| Popular | `Popüler aramalar` |
| Quick access | `Hızlı erişim` |
| Did you mean | `Bunu mu demek istediniz?` |
| Permission note | `Yalnızca yetkinizdeki kayıtlar gösterilir.` |
| Error | `Arama şu anda yanıt vermiyor. Tekrar deneyin.` |
| Rate limit | `Çok sık arama yaptınız. Birkaç saniye bekleyin.` |
| Timeout | `Arama zaman aşımına uğradı. Daha spesifik bir kelime deneyin.` |
| Freshness | `Son güncelleme: 2 dk önce` |
| Active filters | `2 filtre aktif` |
| Capped | `İlk 100 sonuç gösteriliyor` |
| Keyboard hints | `↑↓ gez · Enter aç · Esc kapat` |

Avoid: `Sorgu`, `Entity`, `Index`, `FTS`, `Token`, `Boolean`, `Wildcard`, `Query`, `Sonuç döndürmedi` (use `Sonuç bulunamadı`).

### 7.15 Visual specs (Tailwind)

| Element | Spec |
|---|---|
| Primary color | `#2b6cb0`; hover `#1e4e8c` |
| Background | `bg-white`; page `bg-slate-50` |
| Border | `border-slate-200`; hover `border-slate-300` |
| Text | `text-slate-900`; secondary `text-slate-500`; muted `text-slate-400` |
| Highlight | `bg-amber-100 text-slate-900 rounded px-0.5` |
| Dropdown | `rounded-xl border border-slate-200 shadow-xl bg-white` |
| Card | `rounded-xl border border-slate-200 px-4 py-3 hover:border-slate-300 hover:shadow-md transition` |
| Selected card | `ring-2 ring-[#2b6cb0] bg-blue-50/40 border-transparent` |
| Input | `h-10 rounded-lg border border-slate-200 pl-10 pr-24 text-sm focus:ring-2 focus:ring-[#2b6cb0]/20 focus:border-[#2b6cb0]` |
| Group header | `text-[11px] font-bold uppercase tracking-wider text-slate-500` |
| Type tabs | `h-11 flex items-center gap-1 border-b border-slate-200; active: border-b-2 border-[#2b6cb0] text-slate-900; inactive: text-slate-500 hover:text-slate-700` |
| Tab count | `ml-1 rounded-full bg-slate-100 px-1.5 py-0.5 text-[11px] font-semibold text-slate-500` |
| `kbd` hint | `rounded border border-slate-300 bg-slate-50 px-1.5 py-0.5 text-[11px] font-mono text-slate-500` |
| Scan animation | 120–140 px; two arcs in `#2b6cb0` and `#7dd3fc` on `#f8fafc`; center Lucide `search` 28 px; 2.4 s / 3.6 s linear rotation; fade in 180 ms |
| Badge | `px-2 py-0.5 rounded-full text-[11px] font-semibold` |
| Animation | 140–180 ms, `ease-out`; shimmer 1.4 s linear; progress 1.2 s loop. Respect `prefers-reduced-motion` and animate only `transform`/`opacity` (§7.17.8) |
| Z-index | Top bar dropdown 3000; mobile overlay 5000; help panel already uses 4000 — coordinate so search sits above it when open |

New CSS should be added to the Tailwind source and compiled; do not hand-edit `static/css/app.css`. Bump `app.asset-version` (via `app.version`) so browsers fetch the new CSS/JS.

### 7.16 Deep links and landing pages

- Every result must have a **record-specific** URL, not a generic module root.
- If a detail page does not exist, add `?focus=<id>` or `#row-<id>` support to the list page and highlight the row on load.
- For customers/suppliers, add read-only detail pages for operational roles, or route to a filtered list (`/sales?search=<code>`, `/procurement?search=<code>`).
- For help results, open the help panel with the `pageKey`; do not navigate to a raw HTML file.
- For page results, navigate to the actual route.
- Preserve the query in a `from=search` parameter so the destination can show a “Aramaya dön” link.
- URL-encode quoted phrases (`q=%22mavi+mermer%22`) and preserve them exactly; do not strip the quotes when navigating to a result or back to search.

### 7.17 Slow-search experience and animation

Search is usually fast (50–300 ms), but it will not always be. The first query after an idle period, a cold cache, a large result set, a reindex in progress, a slow mobile connection, or a database under load can push a request to 1–5 seconds. A blank box or a frozen dropdown in that window feels broken. The design goal is:

> **The user must see a meaningful response within 100 ms, even if the real results take seconds.**

Animation is not decoration here. It is how the interface proves that it heard the user, shows that work is still happening, and keeps the page usable while it waits.

#### 7.17.1 Timing model

Every request passes through the same staged feedback model. Times are measured from the moment the user stops typing (after the 200 ms debounce).

| Stage | Time after debounce | What the user sees | What is happening |
|---|---|---|---|
| 0 — Acknowledge | 0–100 ms | Search icon becomes a small spinner; a 2 px progress bar appears under the input; previous results stay visible but dim | Request sent; no results yet |
| 1 — Structure | 100–250 ms | Top bar/dropdown: skeleton group header + 2–3 skeleton rows (only if no previous results). Full page: wait for the scan at 300 ms | Query executing |
| 2 — Reassure | 800 ms | Inline text under the input: `Biraz uzun sürüyor…` | Query still executing |
| 3 — Explain | 2.5 s | `Hâlâ arıyoruz, lütfen bekleyin.` + `Aramayı iptal et` button | Query still executing; user can cancel |
| 4 — Partial | 4 s | Render any groups that arrived (if the API supports partial/streaming); show a subtle `Sonuçlar geliyor…` | Some groups ready |
| 5 — Fallback | 6 s | Keep the spinner and copy; offer `Daha basit bir arama deneyin` | Query nearing the 8 s timeout |
| 6 — End | 8 s | Timeout state with retry and a narrower-search hint | Request aborted |

Rules:

- **Never wait for the server to show feedback.** Stage 0 is local and immediate.
- **Do not show a skeleton if the response arrives in under ~120 ms.** A skeleton that flashes for 80 ms looks like a glitch. Keep the old results and swap them directly.
- **Do not show a full-page overlay or block the input.** The user must be able to keep typing, clear, or cancel.
- **Never show a fake percentage.** An indeterminate progress bar is honest; `%37` is not.
- **After 8 seconds, stop.** A request that never ends is worse than a clear timeout.
- On `/search`, when there are no previous results, the full-page scan animation (§7.18.4) replaces the skeleton stage entirely after a 300 ms delay; do not show skeleton then scan. On refinements, keep the previous results dimmed. The top-bar dropdown never uses a full-page takeover.

#### 7.17.2 Progress bar

A thin indeterminate bar directly under the search input (or at the very top of the dropdown):

```
┌──────────────────────────────────────────────────────────┐
│ 🔍  mermer                                     [⌘K]  ✕  │
│ ██████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │  ← 2px, brand color
└──────────────────────────────────────────────────────────┘
```

- Height 2 px, color `#2b6cb0`, track `bg-slate-100`.
- The filled segment travels left-to-right over 1.2 s, easing `cubic-bezier(0.4, 0, 0.2, 1)`, and repeats while pending.
- On response: jump to 100% over 150 ms, then fade out over 200 ms. This gives a satisfying “done” signal without a layout shift.
- On the full results page, the same bar sits under the page header, not at the top of the viewport, so it is visible after scrolling.
- `role="progressbar"` with `aria-valuetext="Aranıyor"`; hide the visual bar from screen readers and rely on the live region instead.

#### 7.17.3 Skeletons

Skeletons must match the final layout so the page does not jump when results arrive.

```
┌──────────────────────────────────────────────────────────┐
│  ▓▓▓▓▓▓  12                                              │  ← group header
│  ┌────────────────────────────────────────────────────┐  │
│  │  ▢   ▓▓▓▓▓▓▓▓▓▓▓▓▓▓          ▓▓▓▓▓▓               │  │
│  │      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                          │  │
│  │      ▓▓▓▓▓▓▓▓▓▓▓                                  │  │
│  └────────────────────────────────────────────────────┘  │
│  ┌────────────────────────────────────────────────────┐  │
│  │  ▢   ▓▓▓▓▓▓▓▓▓▓▓▓            ▓▓▓▓▓▓               │  │
│  │      ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                              │  │
│  └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

- Show **2 groups × 2–3 rows** maximum. More skeletons look like a finished but broken list.
- Match the real card height (72–80 px) and padding exactly.
- Shimmer: a light gradient sweeps left-to-right every 1.4 s. Use `background-position`, not `box-shadow` or `width`, so it stays on the compositor.
- Skeleton blocks: `bg-slate-200` on `bg-slate-100`, radius matching the card.
- Do not animate the icon or badge placeholders separately; one shimmer per card is enough.
- On the full results page, skeleton the result column only; keep the filter sidebar interactive with cached facet counts if available.

#### 7.17.4 Keeping previous results visible

The fastest-feeling search is the one that never clears the screen.

- When the user types again, keep the previous results mounted and apply `opacity-60` plus a very subtle `blur-[0.5px]` (optional) while the new request is in flight.
- Dim the whole result list, not each card individually, to avoid a flickering mosaic.
- When the response arrives, cross-fade the old list out and the new list in over 120 ms. Do not animate height.
- If the new query returns zero results, keep the old list visible until the response arrives, then replace it with the no-results state. Never flash “no results” while loading.
- Keep scroll position on the full results page. If the result set shrinks above the viewport, scroll to the top smoothly only when the user changed filters, not on every keystroke.

#### 7.17.5 Reveal animation for results

A small entrance animation makes results feel like they arrived, not like they were always there.

- First render of a result set: each card fades in and moves up 4 px (`opacity 0 → 1`, `translateY(4px) → 0`), 160 ms, `ease-out`.
- Stagger 25 ms per card, capped at 6 cards (150 ms total). Do not stagger 20 cards; that feels slow.
- Group headers fade in first, then their cards.
- Subsequent keystrokes: **no entrance animation**, only the cross-fade from 7.17.4. Repeated slide-ins on every keystroke are distracting.
- Counts in group headers can count up quickly (120 ms) only on the full results page, not in the dropdown.

#### 7.17.6 “Still working” copy and cancel

Long waits need words, not just motion. Escalate the copy so the user knows the system is alive:

| Elapsed | Copy | Action |
|---|---|---|
| 0–0.8 s | `Aranıyor…` | — |
| 0.8–2.5 s | `Biraz uzun sürüyor…` | — |
| 2.5–6 s | `Hâlâ arıyoruz, lütfen bekleyin.` | `Aramayı iptal et` |
| 6–8 s | `Neredeyse tamam…` | `Aramayı iptal et` |
| 8 s+ | `Arama zaman aşımına uğradı. Daha spesifik bir kelime deneyin.` | `Tekrar dene` |

- The cancel button aborts the request with `AbortController`, restores the previous results, and focuses the input.
- Do not use a modal or toast for these messages. They belong inline, under the input or at the top of the results.
- Keep the copy calm. Avoid exclamation marks and blame (“Sunucu yanıt vermiyor” is fine; “Hata!” is not).

#### 7.17.7 Partial and progressive results (optional, Phase 2)

If the full grouped query can take seconds, return results progressively instead of waiting for everything:

1. **Fast group first:** the API returns the top 3 groups from the indexed projection in one quick query; the remaining groups load in a second request. The UI renders the first groups immediately and shows a small `Diğerleri aranıyor…` row.
2. **Streaming (SSE):** `GET /api/search/stream` emits one event per group as it completes. The UI appends groups with the same entrance animation. Use this only if measurements show the single query regularly exceeds 1 s; it adds complexity to caching, permissions, and error handling.
3. **Two-phase query on the server** (§9.1) is preferable to client streaming: exact + FTS first, trigram fallback second. It keeps the API simple and still cuts the long tail.

Do not ship streaming in the MVP. Start with the staged loading UI; add progressive groups only when real latency data justifies it.

#### 7.17.8 Animation specs

| Animation | Duration | Easing | Properties | Tailwind / CSS |
|---|---|---|---|---|
| Progress bar sweep | 1.2 s loop | `cubic-bezier(0.4, 0, 0.2, 1)` | `transform: translateX()` | `@keyframes search-progress` |
| Progress complete | 150 ms | `ease-out` | `transform`, `opacity` | `transition` |
| Skeleton shimmer | 1.4 s loop | `linear` | `background-position` | `@keyframes search-shimmer` |
| Result fade-up | 160 ms | `ease-out` | `opacity`, `transform` | `motion-safe:animate-search-in` |
| Result stagger | 25 ms × up to 6 | — | `animation-delay` | inline `style="animation-delay: 25ms"` |
| List cross-fade | 120 ms | `ease-out` | `opacity` | `transition-opacity` |
| Dropdown open | 140 ms | `ease-out` | `opacity`, `translateY(-4px)` | `motion-safe:animate-search-pop` |
| Dropdown close | 100 ms | `ease-in` | `opacity` | `transition-opacity` |
| “Still working” copy | 180 ms | `ease-out` | `opacity` | `transition-opacity` |
| Count up (results page) | 120 ms | `ease-out` | text content only | JS `requestAnimationFrame` |

Reference CSS (add to the Tailwind source, then compile):

```css
@keyframes search-progress {
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(400%); }
}

@keyframes search-shimmer {
  0%   { background-position: -400px 0; }
  100% { background-position: 400px 0; }
}

@keyframes search-in {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}

.search-progress-bar {
  animation: search-progress 1.2s cubic-bezier(0.4, 0, 0.2, 1) infinite;
}

.search-skeleton {
  background-image: linear-gradient(90deg, #e2e8f0 0%, #f1f5f9 50%, #e2e8f0 100%);
  background-size: 800px 100%;
  animation: search-shimmer 1.4s linear infinite;
}

@media (prefers-reduced-motion: reduce) {
  .search-progress-bar,
  .search-skeleton,
  .search-result-enter {
    animation: none;
  }
}
```

Use `motion-safe:` / `motion-reduce:` variants rather than duplicating media queries in every component. Only animate `transform` and `opacity`; animating `height`, `width`, `box-shadow`, or `filter` causes layout/paint work and makes a slow search feel slower.

#### 7.17.9 Accessibility

- The input gets `aria-busy="true"` while a request is in flight and `aria-busy="false"` when it settles.
- The result count region uses `aria-live="polite"` and announces only the final count (`37 sonuç bulundu`), not every skeleton or stage change. Announcing “Aranıyor” on every keystroke is noise.
- The cancel button is reachable by keyboard and has `aria-label="Aramayı iptal et"`.
- Do not move focus when results arrive. The user may be mid-typing.
- Under `prefers-reduced-motion: reduce`, disable the progress sweep, shimmer, and slide-in. Keep a static progress bar and the text stages, because the text is the accessible signal.
- Extended waiting copy must not be the only signal; `aria-busy` and the live region carry the same meaning.
- Do not use flashing or high-contrast strobing animations (seizure risk).

#### 7.17.10 What not to do

| Anti-pattern | Why it hurts |
|---|---|
| Full-screen spinner overlay | Blocks typing and makes the app feel frozen |
| Infinite spinner with no copy | User cannot tell slow from broken |
| Fake percentage | Erodes trust when it stalls at 90% |
| Skeleton flash for fast responses | Looks like a rendering bug |
| Bouncing dots for 3+ seconds | Cute once, irritating on the fifth search |
| Result list that clears on every keystroke | Causes layout jumps and lost context |
| Staggered animation on every keystroke | Makes fast search feel slower |
| Animating `height` or `box-shadow` | Jank on low-end tablets in the yard |
| Blocking the input while searching | Prevents correcting a typo |
| Auto-retry without telling the user | Duplicate load and confusing results |

#### 7.17.11 Measuring the wait

Instrument both the real and the perceived wait:

- `search.time_to_first_feedback_ms` — from keyup to progress bar visible. Target < 100 ms.
- `search.time_to_results_ms` — from request to rendered results. Target p95 < 300 ms.
- `search.time_to_first_group_ms` — for progressive rendering.
- `search.abandoned_after_ms` — requests cancelled by the user.
- `search.timeout.count` — 8 s timeouts.
- `search.slow_queries` — normalized query + duration for queries over 1 s, sampled and privacy-reviewed.

Log slow queries with their `tookMs`, selected types, visibility group, and result count. Review weekly. If the same query is consistently slow, fix the query or the index, not the animation.

#### 7.17.12 QA checklist

- [ ] Feedback appears within 100 ms on a throttled 3G profile.
- [ ] No skeleton flash when the response is under 120 ms (use a mocked fast response).
- [ ] Previous results stay visible and dimmed during a new request.
- [ ] `Biraz uzun sürüyor…` appears at 800 ms and `Hâlâ arıyoruz…` at 2.5 s (mock a slow response).
- [ ] Cancel aborts the request and restores the previous results.
- [ ] Timeout at 8 s shows retry and keeps the query text.
- [ ] Zero-result response does not flash while loading.
- [ ] No layout shift when results replace skeletons (compare CLS).
- [ ] Enter/arrow keys still work while loading.
- [ ] `prefers-reduced-motion` disables motion but keeps the text stages.
- [ ] Screen reader announces the final count once, not every keystroke.
- [ ] No animation jank on a low-end Android tablet (Chrome performance panel, no long tasks > 50 ms).
- [ ] Mobile overlay keeps the progress bar under the sticky header.
- [ ] The animation code respects the asset version bump and is served from the compiled CSS.

### 7.18 Reference patterns from the existing internal search

Three screenshots from another internal application were used as a reference. They are closer to the target experience than the current flat dropdown. The analysis below separates what is worth copying from what should be adapted. Personal names and record titles from the screenshots are replaced with generic examples; the screenshots themselves are not checked into the repository.

#### 7.18.1 What the screenshots do well

| Pattern | Why it works | Adopt? |
|---|---|---|
| Query echo + count + duration: `Yaklaşık 223 sonuç (0,21 saniye)` | Sets expectations, proves the search ran, and makes fuzzy results honest | **Yes**, compact as `Yaklaşık 223 sonuç · 0,21 sn` |
| Horizontal type tabs with counts: `Tümü`, `Malzeme (12)`, `Envanter (16)`, … | One-click narrowing plus a map of the result set; users see where the mass is before scrolling | **Yes**, as the primary type filter |
| Large result cards with type eyebrow, highlighted title, context, snippet, footer | Easier to scan than a dense table; the highlighted term explains the match | **Yes**, with stricter content hygiene |
| Selected card with a strong border | Clear keyboard/selection state | **Yes**, but use a brand ring, not black |
| Full-page scan animation with query echo and stage copy | Makes a slow search feel intentional and explains what is happening | **Yes**, only on `/search` and only after a delay |
| Calm landing input with examples and `Ctrl + K` hint | Teaches what can be searched without a manual | **Yes**, for the empty `/search` page |
| Disabled `Ara` button while searching | Prevents duplicate submits | **Yes** |
| Two-row tab strip | Shows many categories at once | **No**, use one scrollable row + `Diğer ▾` |
| Raw Markdown and emoji in titles (`**`, `⏳`) | — | **No**, strip/clean at index time |
| Duplicate type label (eyebrow + chip) and repeated snippet | Wastes vertical space and looks unpolished | **No** |

#### 7.18.2 Tabbed results: the strongest idea in the reference

The tabs turn a flat result list into a set of shelves. A user who searches `mermer` immediately sees how the matches split across blocks, slabs, invoices, and projects, instead of scrolling a single mixed list. That is information, not just navigation.

**Adopt this behavior:**

- `Tümü` is the default: it shows grouped sections (`BLOKLAR 12`, `PLAKALAR 9`, …).
- Selecting a type switches to a flat, relevance-sorted list for that type. The grouping headers disappear; the tab is the context.
- Tabs are **single-select**. Multi-select type filtering is a power feature that adds cognitive load; keep it out of the first release.
- Each tab shows a count. Counts must be consistent: the `Tümü` count equals the sum of the visible tabs plus `Diğer`, or the UI explicitly says that a record can appear in more than one category.
- The rail is sticky under the search header; it stays visible while the user scrolls results.
- Show at most 8 tabs. Collapse the rest into `Diğer (n) ▾`. On mobile, scroll horizontally with a fade at the edge; never wrap to a second row.
- Keyboard: `←`/`→` moves between tabs, `Home`/`End` jumps to the ends, `Enter`/`Space` activates. Use the ARIA tabs pattern.
- Zero-count tabs are disabled with a tooltip: `Bu türde sonuç yok`.
- Counts reflect the current query and non-type filters. They do **not** change when the user switches tabs, so the numbers stay stable while exploring.
- Put the type tabs above the results and remove type checkboxes from the sidebar. Do not duplicate the same filter in two places.

**Do not copy the two-row tab strip.** It looks tidy in a wide screenshot but breaks on laptops and tablets, and the second row is easy to miss. One scrollable row with an overflow menu is more predictable.

#### 7.18.3 Result cards: rich, but each line must add information

The reference card hierarchy, adapted to ERP vocabulary, is good:

```
Blok                                    ← type eyebrow
BLK-2026-001                            ← highlighted title
[BLOK]                                  ← type chip (redundant)
Beyaz Mermer · Ocak A · 12,4 ton        ← context
BLK-2026-001 stokta, 2 gün önce …       ← snippet (repeats title)
Blok · 2 gün önce güncellendi           ← footer
```

Adopt the hierarchy, but fix these issues:

1. **Strip Markdown and HTML at index time.** The screenshot shows literal `**` and emoji in titles and snippets. Users should never see the source formatting syntax. Normalize to plain text and keep the original record intact in its own module.
2. **Do not duplicate the type.** The eyebrow already says `Blok`; a second `BLOK` chip is noise. One type label is enough.
3. **Do not repeat the title in the snippet.** If the snippet is the same sentence as the title, drop it. Each line should answer a different question: what is it, what context, what detail, when was it updated.
4. **Use dark titles with a yellow highlight, not blue link text.** Blue everywhere makes the list look like a wall of links and reduces scannability. Use brand color for the focused/selected state and the “Aç” action, not for every title.
5. **Use a brand ring for selection, not a 2 px black border.** Black reads as an error or a modal. `ring-2 ring-[#2b6cb0] bg-blue-50/40` is clear and consistent with the rest of the ERP.
6. **Cap the snippet at two lines.** Cards should be scannable; the detail page is one click away.
7. **Show the type only once, the date once, and the status once.** Repetition makes a result look longer than it is.

#### 7.18.4 Full-page scan animation: delight with a budget

The second screenshot shows a centered scanner with three concentric arcs, the query echoed as `"mavi mermer" aranıyor`, a stage line (`Bloklar ve plakalar taranıyor...`), three dots, and a thin progress bar. The `Ara` button is disabled.

**What works:**

- The query is echoed, so the user knows what is being searched.
- The stage copy says what is happening, not just “loading”.
- The page is calm; there is no spinner in the corner and no blank white screen.
- The search button is disabled, preventing duplicate submits.

**What to change:**

- **Delay it.** Do not show the full-page animation for a 120 ms response. Wait 300 ms; if results arrive before that, render them directly. The animation is for the long tail, not the common case.
- **Full page only.** The top-bar dropdown uses skeleton rows and a progress bar (§7.17), never a full-screen takeover. Keep the header and type tabs visible; the scan replaces only the result area.
- **Keep the input editable.** The user must be able to fix a typo or cancel. Disable only the `Ara` button.
- **Use it for the first query, not every refinement.** If the user edits an existing result page, keep the previous results dimmed and show the inline progress bar (§7.17.4); do not replace the page with the scan again. The scan is for the moment when there is nothing to preserve.
- **Show only real stages.** If the backend runs one query, do not cycle through fake “locations”, “warehouses”, and “documents” phases. Use one truthful line: `Tüm kayıtlarda aranıyor…`. If the backend genuinely searches source groups in sequence, show those phases.
- **One primary progress signal.** The scanner plus the progress bar is enough. The three dots are a third signal; drop them, or turn them into a real source counter (`3 / 5 kaynak tarandı`) only if that number is true.
- **Offer an exit.** After 2.5 s, show `Aramayı iptal et`. After 8 s, switch to the timeout state.
- **Respect reduced motion.** With `prefers-reduced-motion: reduce`, show a static search icon, the query, the stage text, and the progress bar; no rotating arcs.

**Animation spec:**

- Scanner: 120–140 px, centered; two arcs in `#2b6cb0` and `#7dd3fc`, rotating in opposite directions at 2.4 s and 3.6 s, linear, no bounce. The reference uses three arcs; two is enough and reduces visual noise.
- Center: Lucide `search` icon, 28 px, `text-slate-700`.
- Background: `#f8fafc`; no card, no shadow.
- Fade in: 180 ms, `ease-out`; fade out on results: 120 ms.
- Query title: `text-lg font-semibold text-slate-900`.
- Stage copy: `text-sm text-slate-500`, changes only when the stage actually changes.
- Progress bar: 2 px, `#2b6cb0` on `#e2e8f0`, same indeterminate behavior as §7.17.2.
- No looping scale, pulse, or bounce on the icon. The arcs are enough.

**Own commentary:** the animation is a garnish, not the meal. If it appears for 150 ms and disappears, it is a flash; if it stays for 5 seconds, it must explain what is happening and offer a way out. The reference gets the tone right; it just uses one signal too many and risks showing a full-page takeover for a search that is usually fast.

#### 7.18.5 Merged layout for Özerler Mega Search

```
┌──────────────────────────────────────────────────────────────────────┐
│  🔍  "mavi mermer"                                      [Ara]  ✕   │
│  Yaklaşık 37 sonuç · 0,04 sn                  [En alakalı ▾]        │
│  [Tam ifade: "mavi mermer" ✕]  [Blok ✕]  [Stokta ✕]                │
│  [Tümü 37] [Blok 12] [Plaka 9] [Proje 5] [Fatura 4] [Müşteri 3] [Diğer 4 ▾]    │
├──────────────────────────────────────────────────────────────────────┤
│  BLOCKLAR  12                                       Tümünü gör      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  BLOK                                                         │  │
│  │  BLK-2026-001                                      [Stokta]   │  │
│  │  Beyaz Mermer · Ocak A · 12,4 ton                             │  │
│  │  Son güncelleme 2 gün önce                                    │  │
│  └────────────────────────────────────────────────────────────────┘  │
│  ...                                                                 │
└──────────────────────────────────────────────────────────────────────┘
```

The exact-phrase chip appears only when quotes were used. The type tabs are the primary type filter; the sidebar keeps status, date, unit, amount, and party filters.

#### 7.18.6 Clean landing: first impression

The third screenshot is the best of the three for the empty state:

- A large pill input with a clear placeholder: `Malzeme kodu, proje, envanter, talep, belge arasında arayın`.
- A single instruction line: `Aramaya başlamak için malzeme kodu, lokasyon, depo, firma, sefer veya talep yazın.`
- A visible shortcut: `İpucu: Ctrl + K ile her sayfadan açabilirsiniz.`
- No tabs, no filters, no empty result table.

Adopt the calm layout and the teaching tone. Adapt the details:

- Use the ERP’s vocabulary: `Blok, plaka, proje, fatura veya müşteri ara…`.
- Keep the placeholder under ~70 characters; move the full list of examples to the helper line.
- Use the brand blue `Ara` button, not black; the black button is stylish but disconnected from the ERP palette.
- Input height 56 px on desktop, 48 px on mobile; fully rounded pill (`rounded-full`), search icon 20 px inside the left, clear button 44×44 px, `Ara` button 96 px wide.
- Placeholder `text-slate-400`; entered text `text-slate-900`; focus ring `ring-2 ring-[#2b6cb0]/20`.
- Disable `Ara` until 2 characters, and show a gray disabled state during loading.
- Put recent, popular, and quick links below the instruction, never inside the input.
- Add the exact-phrase hint here, once: `Tam ifade için tırnak kullanın: "mavi mermer".` Do not repeat it on every screen.

#### 7.18.7 Acceptance criteria for the reference patterns

- [ ] Type tabs show counts, are keyboard navigable, sticky, URL-driven, and overflow to `Diğer ▾` on narrow screens.
- [ ] `Tümü` shows grouped sections; selecting a type shows a flat, relevance-sorted list.
- [ ] Tab counts are consistent with the total, and do not jump when switching tabs.
- [ ] The summary bar shows approximate count and duration in Turkish formatting.
- [ ] Exact-phrase chips appear only when quotes are used and can be removed without clearing the rest of the query.
- [ ] Result cards never show raw Markdown, HTML, or duplicate type labels.
- [ ] Selected cards use a brand ring, not a black border.
- [ ] Full-page scan animation waits 300 ms, appears only for the first query, keeps the input and type tabs visible, offers cancel, and uses only truthful stage copy.
- [ ] Reduced motion replaces the scanner with a static icon and progress bar.
- [ ] Landing page shows examples, shortcut hint, and recent/popular links; no filters before the first query.
- [ ] Visual regression covers landing, tabs, selected card, exact-phrase results, scan animation, and reduced motion.

---

## 8. API contract

### 8.1 Endpoints

| Method | Path | Purpose | Auth |
|---|---|---|---|
| `GET` | `/api/search` | Full results with groups, facets, pagination | Authenticated |
| `GET` | `/api/search/suggest` | Typeahead: top 3 per type, 12 total | Authenticated |
| `GET` | `/api/search/facets` | Counts for filters | Authenticated |
| `POST` | `/api/search/click` | Record a click for analytics | Authenticated + CSRF |
| `GET` | `/search` | Server-rendered full results page | Authenticated |
| `GET` | `/admin/search/analytics` | Admin stats page | `ADMIN` |
| `POST` | `/admin/search/reindex` | Start a full reindex | `ADMIN` |
| `GET` | `/admin/search/status` | Index status JSON | `ADMIN` |

Keep `GET /admin/dashboard/api/search` as a **deprecated adapter** for one release. It can call `SearchService` and map to the old `GlobalSearchResponse`. Remove it after the top bar and dashboard no longer use it.

### 8.2 Request parameters

| Param | Type | Default | Notes |
|---|---|---|---|
| `q` | string | — | 2–200 chars; trim; reject control characters. Supports only one syntax: `"exact phrase"` (ASCII or typographic quotes) |
| `type` | CSV | all visible | `BLOCK,SLAB,PROJECT` |
| `status` | CSV | — | Allowlisted per type |
| `date` | enum | — | `7d`, `30d`, `ytd`, `custom` |
| `dateFrom`, `dateTo` | ISO date | — | Required when `date=custom` |
| `unit` | enum | — | `QUARRY`, `FACTORY`, `WORKSHOP`, `SITE` |
| `minAmount`, `maxAmount` | decimal | — | Invoices/orders |
| `sort` | enum | `relevance` | `relevance`, `newest`, `code` |
| `limit` | int | 20 | Max 50 |
| `cursor` | string | — | Opaque keyset cursor |

`q` examples:

| Query | Meaning |
|---|---|
| `mavi mermer` | Broad search; fuzzy and synonyms allowed |
| `"mavi mermer"` | Exact phrase; no fuzzy, no synonyms |
| `fatura "mavi mermer"` | Invoice type hint + exact phrase |
| `"mavi mermer" "ocak a"` | Both phrases must match |
| `"mermer` | Unclosed quote: handled as plain text with a UI hint; not a 400 error |

Do not accept `tenantId`, `visibility`, or raw SQL fragments. Do not add `AND`, `OR`, `-`, wildcards, or field prefixes; quotes are the only supported syntax.

### 8.3 Response

```json
{
  "query": "\"beyaz mermer\"",
  "normalizedQuery": "beyaz mermer",
  "interpretation": {
    "phrases": ["beyaz mermer"],
    "terms": [],
    "mode": "phrase",
    "hasUnclosedQuote": false
  },
  "total": 37,
  "approximate": true,
  "countKind": "approximate",
  "tookMs": 42,
  "indexUpdatedAt": "2026-11-25T09:14:03Z",
  "degraded": false,
  "groups": [
    {
      "type": "BLOCK",
      "label": "Blok",
      "icon": "box",
      "total": 12,
      "items": [
        {
          "id": "1024",
          "title": "BLK-2026-001",
          "subtitle": "Beyaz Mermer · Ocak A · 12,4 ton",
          "url": "/genealogy?code=BLK-2026-001",
          "score": 0.93,
          "status": "IN_STOCK",
          "statusLabel": "Stokta",
          "date": "2026-11-20",
          "amount": null,
          "highlights": {},
          "phraseHighlights": { "subtitle": [[0, 12]] }
        }
      ]
    }
  ],
  "facets": [
    { "key": "type", "label": "Tür", "values": [
      { "value": "BLOCK", "label": "Blok", "count": 12 },
      { "value": "SLAB", "label": "Plaka", "count": 9 }
    ]},
    { "key": "status", "label": "Durum", "values": [
      { "value": "IN_STOCK", "label": "Stokta", "count": 8 }
    ]}
  ],
  "suggestions": ["mermer ocak", "mermer plaka"],
  "nextCursor": null
}
```

- `interpretation` drives the query chips (`Tam ifade: "mavi mermer"`) and the “Tırnakları kaldır” recovery. Do not make the client parse the query independently; the server is the single source of truth.
- `approximate=true` and `countKind="approximate"` render `Yaklaşık N sonuç`. Use `countKind="exact"` for exact/prefix-only searches.
- `phraseHighlights` are the ranges for the whole quoted phrase; `highlights` covers individual term matches. The client merges them and never uses `innerHTML`.
- `tookMs` is server-side query time. The client may display `0,21 sn`. `indexUpdatedAt` powers “Son güncelleme”. `degraded=true` means the fallback engine or a partial result set was used; show a subtle notice.

### 8.4 Suggest response

Same shape but `limit=3` per group and no facets; `total` is the true total per group. Keep the payload under 20 KB.

For quoted input, suggestions are restricted to the current phrase; do not append unrelated type keywords inside the quote. If the quote is unclosed, return `interpretation.hasUnclosedQuote=true` and let the UI show the closing-quote hint instead of guessing.

### 8.5 Error format

Use the existing `BackEndResponse` conventions only for the deprecated adapter. For the new API, return a small JSON error:

```json
{ "error": "QUERY_TOO_LONG", "message": "Arama metni en fazla 200 karakter olabilir." }
```

Additional error codes:

| Code | Meaning | UI behavior |
|---|---|---|
| `QUERY_TOO_LONG` | More than 200 characters | Inline validation, keep text |
| `INVALID_FILTER` | Unknown type/status/unit value | Ignore the invalid filter, log, keep the rest |

Unclosed quotes, too many phrases, and phrases shorter than 2 characters are **not** errors. Handle them in the parser and show an inline hint; never turn a search attempt into a red error screen.

HTTP statuses: `400` validation, `401` unauthenticated, `403` forbidden, `429` rate limited, `503` degraded/timeout. Never return a stack trace.

---

## 9. Performance and scalability

### 9.1 Query plan

The production query is a single SQL statement, not 13 fan-out queries:

```sql
WITH params AS (
    SELECT
        to_tsquery('simple', :tsQuery) AS tsq,
        :normalized AS raw
),
candidates AS (
    SELECT d.*,
           ts_rank_cd(d.search_vector, p.tsq, 32) AS text_rank,
           similarity(d.search_text, p.raw) AS trgm_rank,
           CASE
               WHEN d.code_norm = p.raw OR d.title_norm = p.raw THEN 1.00
               WHEN d.code_norm LIKE p.raw || '%' THEN 0.80
               WHEN d.title_norm LIKE p.raw || '%' THEN 0.60
               ELSE 0.00
           END AS exact_rank
    FROM search_document d
    CROSS JOIN params p
    WHERE d.tenant_id = :tenantId
      AND d.visibility IN (:visibilities)
      AND (
          d.search_vector @@ p.tsq
          OR d.search_text % p.raw
          OR d.code_norm LIKE p.raw || '%'
      )
)
SELECT c.*,
       (c.text_rank * 0.50
        + c.trgm_rank * 0.20
        + c.exact_rank * 0.15
        + c.type_priority * 0.10
        + (1.0 / (1.0 + EXTRACT(EPOCH FROM (now() - c.source_updated_at)) / 2592000.0)) * 0.05) AS score
FROM candidates c
ORDER BY score DESC, c.source_updated_at DESC, c.id
LIMIT :limit OFFSET :offset;
```

Implementation notes:

- **Two-phase query** for large corpora: run FTS/exact/prefix first; only run the trigram branch if the FTS branch returns fewer than `limit` rows. `OR` across two GIN indexes can be slower than two targeted queries.
- **Phrase queries are a separate plan:** add `AND d.search_vector @@ phraseto_tsquery('simple', :phrase)` for each quoted phrase, skip the trigram `OR`, and compute `phrase_rank` from `strpos`/`position` on the normalized columns. This keeps exact-phrase searches precise and avoids scanning the long tail.
- **Typeahead** adds `row_number() OVER (PARTITION BY doc_type ORDER BY score DESC)` and filters `rn <= 3`.
- **Counts** use `COUNT(*) OVER (PARTITION BY doc_type)` in the same query, or a separate facet query. Do not run a second full search for counts.
- **Keyset pagination** for deep pages: `(score, source_updated_at, id) < (:lastScore, :lastDate, :lastId)`. Offset is fine for the first 10 pages.
- **Short queries:** for 2 characters, skip FTS/trigram and use `code LIKE :q || '%' OR lower(title) LIKE :q || '%'`.
- **Two-level timeout:** use a **soft budget of 800 ms** for the FTS/trigram attempt (`SET LOCAL statement_timeout = '800ms'`). If it fires, cancel, log, and either retry the simple fallback or return partial results with `degraded=true`. Enforce a **hard request budget of 7 s** at the controller. The client gives up at **8 s** to allow for network and rendering. Never let a search request run unbounded.
- **No N+1:** the projection table contains all display fields; do not lazy-load source entities in the mapper.

The client-side staged feedback, progress bar, skeletons, cancel action, and timeout copy are specified in **§7.17**. Server latency and perceived latency are separate SLOs; both are measured.

### 9.2 Indexes

- GIN on `search_vector` for FTS.
- GIN on `search_text gin_trgm_ops` for fuzzy.
- B-tree on `(tenant_id, doc_type, source_updated_at DESC)` for type scans/reindex.
- B-tree on `(tenant_id, visibility)` for ACL filtering.
- B-tree on `(tenant_id, doc_type, status)` and `(tenant_id, doc_type, entity_date DESC)` for filters.
- Optional GIN on a `tags` column if you add free-form tags later.

Monitor index size. Trigram GIN is typically 2–4× the indexed text size. Cap `body` at 4–8 KB per document; do not index entire HTML pages or long notes.

### 9.3 Caching

| Cache | Key | TTL | Notes |
|---|---|---|---|
| Suggest | `normalizedQuery + parsedQueryHash + visibilities + indexVersion` | 30 s | Caffeine, max 10,000 entries |
| Facets | `normalizedQuery + parsedQueryHash + filters + visibilities + indexVersion` | 60 s | Caffeine |
| Full page | `normalizedQuery + parsedQueryHash + filters + visibilities + sort + cursor + indexVersion` | 15 s | Optional; skip if memory pressure |
| Negative | `normalizedQuery + parsedQueryHash + visibilities` when `total=0` | 10 s | Prevents hammering on typos |
| HTTP | `ETag` = hash(indexVersion + normalizedQuery + parsedQueryHash + filters) | 10 s | `Cache-Control: private, max-age=10` |

Include the **index version** in every key so a reindex invalidates caches without manual flushing. Never cache across different visibility groups. Do not use a shared CDN cache for authenticated search results.

### 9.4 Connection pool and rate limits

- The current production Hikari pool is `DB_POOL_MAX=5`. The new single-query design uses one connection per search, but concurrent users can still exceed 5. Raise to `10–15` if metrics show wait time; do not raise blindly.
- Add a search-specific rate limit: 20 requests per 10 seconds per authenticated user, burst 10. Return `429` with `Retry-After`. Reuse the existing `RateLimitFilter` pattern or add a small filter for `/api/search/**`.
- Reject queries longer than 200 characters before hitting the database.
- Reject more than 5 types in a single request (or allow all, but cap the SQL `IN` list).
- Use `AbortController` on the client; cancel stale requests server-side if possible (Spring MVC async or a simple sequence check).

### 9.5 Monitoring and health

Add Micrometer metrics:

- `search.query.duration` (timer, tags: `endpoint`, `type`, `result=hit|zero|error`)
- `search.query.count` (counter)
- `search.index.lag.seconds` (gauge)
- `search.index.documents` (gauge, tag `type`)
- `search.index.failures` (counter)
- `search.cache.hit` / `search.cache.miss` (counter)
- `search.outbox.pending` (gauge, Phase 2)

Add `SearchIndexHealthIndicator`:
- `UP` if last incremental sync < 5 minutes and no failed jobs.
- `DEGRADED` if lag > 15 minutes.
- `DOWN` if the index table is empty or the last full reindex failed.

Expose a compact status in `/admin/search/analytics` and a machine-readable `/admin/search/status`.

### 9.6 Scale thresholds

| Corpus size | Expected behavior | Action |
|---|---|---|
| < 100k docs | < 50 ms warm | No special tuning |
| 100k–1M docs | 50–150 ms warm; GIN 100–500 MB | Monitor; tune `shared_buffers`, `maintenance_work_mem` |
| 1M–5M docs | 100–300 ms; consider two-phase query and partitioning by `doc_type` | Tune autovacuum, `gin_pending_list_limit`; consider read replica |
| > 5–10M docs, or p95 > 300 ms after tuning | Postgres may still work, but relevance/facets get harder | Evaluate OpenSearch/Meilisearch; or partition by tenant/type |
| Multi-tenant SaaS | Per-tenant isolation, noisy-neighbor concerns | External engine or per-tenant schema/index |

### 9.7 Reindexing

- **Incremental:** every 30 s from `updated_date` watermark; batch 500; upsert.
- **Nightly reconcile:** full scan of source IDs vs `search_document`; delete orphans; repair missing docs; log drift.
- **Full reindex:** build into `search_document_new` and swap in a transaction, or use an `index_version` column and switch the active version. For < 1M docs, a truncate+rebuild in a maintenance window is acceptable; for zero-downtime, use the versioned table.
- **Index build settings:** `maintenance_work_mem = 512MB` for the session; `CREATE INDEX CONCURRENTLY` for large existing tables. Flyway runs migrations in a transaction by default; create the initial indexes on an empty table normally, and use a separate operational script for concurrent rebuilds.
- **After reindex:** `ANALYZE search_document;` and bump `search_index_state.index_version`.

---

## 10. Implementation roadmap

### Phase 0 — Foundations (2–3 days)

- Add `V40__create_search_document.sql` (extension, table, indexes, `search_index_state`, `search_query_log`).
- Add `SearchDocument` JPA entity mapping only portable columns; `search_vector` stays DB-only.
- Add `SearchDocumentRepository`, `SearchIndexStateRepository`, `SearchQueryLogRepository`.
- Add `SearchNormalizer` (Turkish fold, punctuation, max length) and `SearchQueryParser` (quoted phrases, type hints, limits) with unit tests.
- Add `SearchProperties` (`app.search.*`) and a feature flag `app.search.mega.enabled=false`.
- Add `SearchDocumentType` enum and `SearchSource` interface.
- Add a simple `SimpleSearchQueryRepository` for H2/local (`LIKE` on `search_text`) so local development works before Postgres FTS is wired.

### Phase 1 — MVP (1–2 weeks)

- Implement `SearchSource` for 10 core types: `BLOCK`, `SLAB`, `CUT_ITEM`, `PROJECT`, `CUSTOMER`, `SUPPLIER`, `INVOICE`, `SALES_ORDER`, `PURCHASE_ORDER`, `OPERATION_WORK_ORDER`.
- Implement `PostgresSearchQueryRepository` with FTS + trigram + exact-phrase hard filter + visibility + grouping + highlight ranges.
- Implement `SearchIndexService` (batch upsert, delete), `SearchIndexScheduler` (30 s), `SearchReindexService` (admin command).
- Add explicit delete hooks in the hard-delete services for the indexed types.
- Add `SearchApiController` (`/api/search`, `/api/search/suggest`) and `SearchPageController` (`/search`).
- Add `SearchPermissionPolicy` and security tests for `ADMIN`, `EXECUTIVE`, `OPERATOR`, `USER`, anonymous.
- Add the shared `fragments/global-search.html` and `static/js/global-search.js`; replace the inline Alpine code in `navbar.html` and `dashboard.html`.
- Add the clean `/search` landing state, horizontal type tabs with counts, summary bar, and grouped/flat result switching from §7.18.
- Add exact-phrase parsing (`"..."`), the `Tam ifade` chip, mobile quote button, and zero-phrase recovery from §6.2.1.
- Add `Ctrl/Cmd+K`, `/`, `Esc`, arrow keys, recent searches (localStorage), skeleton/empty/error states.
- Add the staged slow-search UX from §7.17: 100 ms progress bar, skeleton shimmer, dimmed previous results, 800 ms/2.5 s copy escalation, cancel, 8 s timeout, reduced-motion support.
- Add the full-page scan animation from §7.18.4 with a 300 ms delay, real stage copy, and a reduced-motion fallback.
- Add Turkish labels for new types in `messages.properties` and `messages_tr.properties`.
- Add read-only customer/supplier detail routes (or filtered-list fallbacks) so results are openable by non-admins.
- Add unit/integration/controller/E2E tests.
- Feature flag off by default; enable for `ADMIN`/`EXECUTIVE` in staging.

### Phase 2 — Full coverage and polish (1–2 weeks)

- Add the remaining Tier 1 types: `PRODUCTION_ORDER`, `COLLECTION`, `CHECK`, `PALLET`, `STOCK_ITEM`, `MACHINE`, `COST_CENTER`, `QUARRY`, `PAGE`, `HELP`.
- Add Tier 2 types: `USER`, `SETTING`, `EMAIL_TEMPLATE`, `REPORT`, `FILE` (metadata only), with visibility.
- Add filters/facets (type, status, date, unit, amount) and URL state.
- Add synonyms table + admin editing; “Bunu mu demek istediniz?”.
- Add sidebar filters (status, date, unit, amount), load more, and sort; the type tabs and grouped/flat switching ship in Phase 1.
- Add Caffeine caches keyed by index version; rate limiting; metrics; health indicator.
- Add admin analytics page and reindex console.
- Add click logging and CTR/zero-result reporting.
- Add `search_outbox` + triggers if freshness/delete correctness needs tightening.
- Add help/page catalog indexing.
- Add progressive/partial group rendering (§7.17.7) if measured p95 justifies it.

### Phase 3 — Advanced (2–4 weeks)

- Near-real-time outbox/CDC (if not already done).
- `pgvector` hybrid search for help/notes.
- Saved searches, alerts (“bu aramada yeni sonuç”), cross-device history.
- Ranking experiments (A/B) using click logs.
- Search analytics improvements: top zero-result queries, content gaps, synonym suggestions.
- Optional external engine if scale thresholds are crossed.

### Concrete file map

```
src/main/java/com/ozerler/marble/
  controller/
    SearchApiController.java              # /api/search, /suggest, /facets, /click
    SearchPageController.java             # /search (Thymeleaf)
    admin/SearchAdminController.java      # /admin/search/analytics, /reindex, /status
  dto/search/
    SearchRequest.java
    SearchResponse.java
    SearchGroup.java
    SearchHit.java
    SearchFacet.java
    SearchSuggestion.java
  model/
    SearchDocument.java                   # portable columns only
    SearchIndexState.java
    SearchQueryLog.java
    SearchSynonym.java
  repository/
    SearchDocumentRepository.java
    SearchIndexStateRepository.java
    SearchQueryLogRepository.java
    SearchSynonymRepository.java
  repository/search/
    SearchQueryRepository.java            # interface
    PostgresSearchQueryRepository.java    # FTS + trigram
    SimpleSearchQueryRepository.java      # H2/local fallback
  service/search/
    SearchService.java
    SearchIndexService.java
    SearchIndexScheduler.java
    SearchReindexService.java
    SearchNormalizer.java
    SearchQueryParser.java
    SearchQueryBuilder.java
    SearchPermissionPolicy.java
    SearchDocumentMapper.java
    SearchSource.java
    SearchSourceRegistry.java
    SearchCache.java
    SearchMetrics.java
    SearchHealthIndicator.java
  config/
    SearchProperties.java
    SearchConfig.java
src/main/resources/
  db/migration/V40__create_search_document.sql
  templates/fragments/global-search.html
  templates/search/results.html
  static/js/global-search.js
  static/css/app.css                     # compiled Tailwind; do not hand-edit
  messages.properties
  messages_tr.properties
```

Package by layer, constructor injection only, business logic in services, no JPA entities returned from controllers. `SearchQueryRepository` may use `NamedParameterJdbcTemplate` because JPQL cannot express `tsvector`/`ts_rank_cd`; keep it behind the repository interface so the controller/service boundary stays clean.

### Suggested libraries

- **No new runtime dependency** for the core search: PostgreSQL FTS + `pg_trgm` + Spring JDBC/JPA.
- **Caffeine** (already in `pom.xml`) for caches.
- **Micrometer** (via Actuator) for metrics.
- **Testcontainers PostgreSQL 16** (already in `pom.xml`) for integration tests.
- **Alpine.js + Lucide + Tailwind** (already in the app) for the UI.
- Optional later: `pgvector` for embeddings; no JS search library needed.

### Testing

- Unit: normalizer (Turkish chars, punctuation, codes, max length), query parser (quoted phrases, smart quotes, apostrophes, unclosed quotes, multiple phrases, limit), query builder (injection attempts, operators), permission policy, grouping, highlight ranges.
- Integration (Testcontainers Postgres 16): FTS, trigram typo, Turkish fold, exact phrase (`phraseto_tsquery`), phrase + Turkish fold, phrase hard filter, visibility filter, type filter, date/status filter, pagination, grouping, `EXPLAIN` sanity.
- H2/local: simple repository fallback.
- Controller security: anonymous `401`/redirect, `USER`/`OPERATOR` allowed, `ADMIN`-only types hidden, `ADMIN` sees all.
- Indexing: upsert/delete idempotency, batch size, reindex, orphan cleanup, index version bump.
- E2E Playwright: `Ctrl+K`, type, grouped results, arrow/Enter, recent searches, no results, filters, mobile overlay, error/retry.
- E2E Playwright: landing state, type tabs (counts, keyboard, overflow, URL), `Tümü` vs selected type, exact phrase (`"mavi mermer"`), unclosed quote hint, remove-quotes recovery, mobile quote toggle.
- Slow-search E2E (mock a 2 s API delay): progress bar appears, previous results stay visible, `Biraz uzun sürüyor…` shows at 800 ms, `Hâlâ arıyoruz…` at 2.5 s, cancel aborts and restores, timeout at 8 s. Full-page scan waits 300 ms and keeps the input editable.
- Visual regression: landing, idle, loading-fast, loading-slow, full-page scan, tabs, selected card, exact-phrase results, no-results, error, reduced-motion, mobile overlay.
- Performance: seed 100k documents; assert p95 < 200 ms for suggest and < 300 ms for full page on CI hardware; record results in the PR. Separately assert time-to-first-feedback < 100 ms.

### Rollout and rollback

1. Ship the schema and indexer with the feature flag off.
2. Index in the background; verify counts/lag in `/admin/search/status`.
3. Enable the new top bar for `ADMIN`/`EXECUTIVE` in staging; compare results with the old search.
4. Enable for all users in production; keep the old endpoint for one release.
5. Monitor p95, zero-result rate, CTR, 429s, and index lag for two weeks.
6. Remove the old `GlobalSearchService`/`GlobalSearchController` and their tests.

Rollback: set `app.search.mega.enabled=false`; the top bar falls back to the old endpoint (or a simple form). The search tables are inert; no data loss. Do not drop the tables during a rollback.

### PR checklist (repo rules)

- Bump `pom.xml` `<version>` by one patch (e.g., `1.0.13` → `1.0.14`) and set `app.version` in `application.yml` to the same value.
- Recompile Tailwind after adding classes; bump the asset version so `?v=` cache-busting changes.
- Export any new shared JS functions on `window` if templates call them.
- Run `./mvnw verify` (Checkstyle, PMD, ArchUnit, JaCoCo) and the Playwright suite.
- Add the new Flyway migration and update `FlywayPostgresSliceTest` expectations.
- Document the feature flag and rollback in the PR description.

---

## 11. Risks and open decisions

| Risk / decision | Mitigation / recommendation |
|---|---|
| Trigram GIN index size and write cost | Cap `body` at 4–8 KB; monitor index size; use two-phase queries; consider `fastupdate` |
| Turkish `I/İ/ı/i` edge cases | Normalize in Java; unit-test `İstanbul`, `ıspanak`, `IŞIK`, `şantiye`; do not rely on DB locale |
| Ranking quality | Start with weights in §6.1; log clicks; tune monthly; do not overfit before data |
| Stale index | 30 s poller + nightly reconcile; add outbox/triggers if freshness becomes critical |
| Permission drift | Central `SearchPermissionPolicy`; security tests per role; audit new `SearchSource` visibility |
| Results link to forbidden pages | Landing-page parity task; add read-only customer/supplier pages or filtered-list fallbacks |
| Production pool is only 5 | Single-query design reduces fan-out; raise `DB_POOL_MAX` to 10–15 if metrics show wait; add rate limit |
| External engine temptation | Stay on Postgres until §9.6 thresholds; document the decision |
| Secrets in the index | Explicit denylist: passwords, TOTP, API keys, SMTP values, setting values, full logs |
| XSS in highlights | Structured ranges + `textContent`; never `innerHTML` with raw indexed text |
| Markdown/HTML artifacts in results | Strip formatting at index time; render plain text; visual regression test with `**`, backticks, and emoji samples |
| Quote syntax discoverability | Show the hint once on the landing page, offer the “tam ifade olarak ara” suggestion, and add a mobile quote button; do not rely on users knowing the syntax |
| Exact phrase surprises | Treat quotes as a hard filter and offer “Tırnakları kaldır” on zero results; never silently broaden a quoted query |
| Tab count drift | Compute counts from the same query snapshot and index version; test that `Tümü` equals the sum of visible types; cache facets for 60 s |
| Animation overuse | Delay full-page animation by 300 ms; use it only on `/search`; respect reduced motion; keep the top-bar feedback lightweight |
| Analytics privacy | 90-day retention; no sensitive query terms; aggregate admin stats; allow opt-out/clear |
| Multi-tenancy | `tenant_id` column now, server-side context later; never accept it from the client |

**Open decisions for product:**

1. Should financial records (`costs`, `expenses`, `invoices`, `collections`) be visible to all authenticated users, or only finance/admin? If only finance/admin, tighten the controller read access first.
2. Should customers/suppliers get read-only detail pages for operational roles, or should search route to filtered sales/procurement lists?
3. Is 30-second index freshness acceptable for launch, or is a 5-second outbox required from day one?
4. Should recent searches be stored server-side for cross-device continuity, or is localStorage enough for v1?
5. Which types belong in the top-bar dropdown (max 4 groups) vs only the full results page?
6. Should quotes remain the only query syntax, or do users also need a visible “Tam ifade” toggle? **Recommendation:** support quotes, show the hint/chip, and add the mobile toggle; do not add `AND`/`OR`/`-` syntax.
7. Which 8 types deserve a permanent tab? **Recommendation:** `Blok`, `Plaka`, `Proje`, `Fatura`, `Müşteri`, `Tedarikçi`, `İş Emri`, `Belge`; everything else under `Diğer`.
8. Is the branded scan animation worth the maintenance, or should `/search` use the same skeleton as the dropdown? **Recommendation:** keep the scan only for the first full-page query, delayed 300 ms; reuse the skeleton everywhere else.

---

## Appendix A — Example user queries and expected behavior

| User types | Expected top result | Why |
|---|---|---|
| `BLK-2026-001` | Block `BLK-2026-001` | Exact code |
| `blk 2026 001` | Same block | Normalizer removes hyphens/spaces |
| `mermr` | Blocks/slabs with `Mermer` | Trigram typo tolerance |
| `santiye` | Projects/`Şantiye` | Turkish fold |
| `ahmet yilmaz` | Customer contact `Ahmet Yılmaz` | Turkish fold + name fields |
| `fatura 12345` | Invoice `12345` | Type keyword + code |
| `vadesi geçen çek` | Overdue checks | Synonym/status keyword + filter hint |
| `beyaz mermer` | White marble blocks/slabs | Stone type + FTS |
| `stoktaki plakalar` | Available slabs | Synonym + status boost |
| `ocak a` | Quarry A blocks | Quarry name/code |
| `12345` | Invoice/order/check with that number | Exact/prefix on codes |
| `mermer` | Blocks, slabs, projects, invoices | Broad FTS; grouped results |
| `"mavi mermer"` | Only records containing that exact phrase | Quotes = hard phrase filter; no typo/synonym expansion |
| `fatura "mavi mermer"` | Invoices containing the exact phrase | Type hint + phrase |
| `"mermer` | Broad results + inline closing-quote hint | Unclosed quote is not an error |

## Appendix B — Configuration

```yaml
app:
  search:
    enabled: true
    engine: postgres            # postgres | simple
    min-query-length: 2
    max-query-length: 200
    suggest-limit: 12
    per-type-limit: 3
    page-size: 20
    max-page-size: 50
    type-tabs-max: 8
    phrase-enabled: true
    max-phrases: 3
    phrase-min-length: 2
    full-page-loading-delay-ms: 300
    cache-ttl-seconds: 30
    facet-cache-ttl-seconds: 60
    index-batch-size: 500
    index-poll-interval-ms: 30000
    reindex-cron: "0 30 3 * * *"
    soft-timeout-ms: 800        # FTS attempt budget; on expiry use fallback/partial
    hard-timeout-ms: 7000       # controller-level request budget
    client-timeout-ms: 8000     # browser AbortController budget (includes network)
    rate-limit:
      requests: 20
      window-seconds: 10
```

`engine: simple` is for H2/local and tests; it uses `LIKE` on `search_text` and does not require `pg_trgm` or `search_vector`. For quoted phrases, it uses `search_text LIKE '%' || :normalizedPhrase || '%'` as a hard filter; this is slower but acceptable for local/test data volumes.

## Appendix C — Migration checklist

- [ ] `CREATE EXTENSION IF NOT EXISTS pg_trgm;` succeeds (trusted extension on PG 16; if not, DBA creates it).
- [ ] `search_document` table and indexes created, including `title_norm`, `subtitle_norm`, `code_norm`, `body_norm`, and `search_text`.
- [ ] `search_vector` is generated from the normalized columns, not the raw display columns.
- [ ] Field separators (`zzsep`) prevent exact phrases from matching across title/subtitle boundaries.
- [ ] Exact-phrase path tested with Turkish folding (`"santiye"` matches `Şantiye`).
- [ ] `search_index_state` singleton row inserted.
- [ ] `search_query_log` table created with retention job.
- [ ] `search_synonym` table seeded with Turkish ERP terms.
- [ ] `SearchDocument` JPA entity validates on Postgres and H2.
- [ ] `FlywayPostgresSliceTest` updated to expect version ≥ 40.
- [ ] `search_vector` is not mapped in JPA; H2 create-drop still works.
- [ ] Indexer backfills existing data before the UI flag is enabled.
- [ ] Admin reindex command tested on a staging copy.
- [ ] Metrics/health indicator visible in `/actuator/health` and `/admin/search/status`.
- [ ] Feature flag off by default; rollout plan agreed.
- [ ] Version bump and Tailwind/asset-version update in the PR.

---

## Appendix D — Why this fits the existing codebase

- **Spring Boot 4.2 / Java 24 / Hibernate 7 / PostgreSQL 16** are already the stack. No new runtime service.
- **Flyway owns the schema.** The new table/indexes fit the existing migration pattern (`V40__...`).
- **`AuditableEntity` gives `updated_date`** on 53 of 56 entities, enabling the watermark poller.
- **Caffeine is already a dependency**, so the cache layer adds no new library.
- **Testcontainers PostgreSQL 16 is already used**, so FTS/trigram integration tests fit the existing test style.
- **Thymeleaf + Alpine + Tailwind + Lucide** are already the UI stack; the command palette needs no React/Vue.
- **`HelpContentCatalog` and the mega menu's `data-search`** provide ready-made UI content to index.
- **`messages.properties`** already has `search.type.*` labels; extending them is low-friction.
- **The existing `GlobalSearchHit`** shape can be mapped to the new response during the adapter period, reducing migration risk.

The result is a search feature that is native to the application, not a second system bolted onto it.
