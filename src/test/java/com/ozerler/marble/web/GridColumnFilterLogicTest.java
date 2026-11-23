package com.ozerler.marble.web;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ozerler.marble.model.Block;
import com.ozerler.marble.util.GridPages;

class GridColumnFilterLogicTest {

    private static final Map<String, String> ALIASES = Map.of(
            "blockCode", "blockCode",
            "quarryName", "quarry.name"
    );

    @Test
    @DisplayName("Unknown or unsafe column keys are ignored")
    void unknownKeysAreIgnored() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cf.blockCode", "BLK-1");
        request.setParameter("cf.unknown", "x");
        request.setParameter("cf.drop;table", "x");
        request.setParameter("cf.quarryName", "  ");
        GridColumnFilters filters = GridColumnFilters.from(request, ALIASES);
        Assertions.assertThat(filters.getItems()).hasSize(1);
        Assertions.assertThat(filters.find("blockCode").getValue()).isEqualTo("BLK-1");
        Assertions.assertThat(filters.isActive("unknown")).isFalse();
    }

    @Test
    @DisplayName("Default operator is İçerir / contains")
    void defaultOperatorIsContains() {
        Assertions.assertThat(GridFilterOperator.parse(null)).isEqualTo(GridFilterOperator.CONTAINS);
        Assertions.assertThat(GridFilterOperator.parse("")).isEqualTo(GridFilterOperator.CONTAINS);
        Assertions.assertThat(GridFilterOperator.parse("equals").getLabel()).isEqualTo("Eşittir");
        Assertions.assertThat(GridFilterOperator.parse("starts").getLabel()).isEqualTo("İle Başlar");
        Assertions.assertThat(GridFilterOperator.parse("not_contains").getLabel()).isEqualTo("İçermez");
    }

    @Test
    @DisplayName("LIKE wildcards and JPQL quotes are escaped")
    void likeWildcardsAndQuotesAreEscaped() {
        Assertions.assertThat(GridLikeEscapes.escape("a%b_c\\d")).isEqualTo("a\\%b\\_c\\\\d");
        Assertions.assertThat(GridLikeEscapes.likePattern("AB", GridFilterOperator.CONTAINS)).isEqualTo("%AB%");
        Assertions.assertThat(GridLikeEscapes.likePattern("AB", GridFilterOperator.STARTS_WITH)).isEqualTo("AB%");
        Assertions.assertThat(GridLikeEscapes.jpqlLiteral("O'Hara")).isEqualTo("O''Hara");
    }

    @Test
    @DisplayName("JPQL predicate uses CAST/LOWER and LIKE ESCAPE")
    void jpqlPredicateEscapesAndUsesLikeEscape() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cf.blockCode", "BLK%1");
        request.setParameter("co.blockCode", "contains");
        GridColumnFilters filters = GridColumnFilters.from(request, ALIASES);
        String sql = GridColumnFilterJpql.predicate("b", ALIASES, filters);
        Assertions.assertThat(sql).contains("LOWER(CAST(b.blockCode AS string))");
        Assertions.assertThat(sql).contains("LIKE '%blk\\%1%' ESCAPE '\\'");
        Assertions.assertThat(sql).doesNotContain("LIKE '%blk%1%'");
    }

    @Test
    @DisplayName("Equals and not-contains render the expected JPQL")
    void equalsAndNotContainsRenderExpectedJpql() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cf.blockCode", "BLK");
        request.setParameter("co.blockCode", "equals");
        request.setParameter("cf.quarryName", "Ocak");
        request.setParameter("co.quarryName", "not_contains");
        String sql = GridColumnFilterJpql.predicate("b", ALIASES, GridColumnFilters.from(request, ALIASES));
        Assertions.assertThat(sql).contains("LOWER(CAST(b.blockCode AS string)) = 'blk'");
        Assertions.assertThat(sql).contains("b.quarry.name IS NULL OR");
        Assertions.assertThat(sql).contains("NOT LIKE '%ocak%' ESCAPE '\\'");
    }

    @Test
    @DisplayName("Query rewriter appends the predicate once a context is open")
    void rewriterAppendsPredicateWhenContextIsOpen() {
        String query = "SELECT b FROM Block b WHERE (:search IS NULL)";
        GridColumnFilterRewriter rewriter = new GridColumnFilterRewriter();
        Assertions.assertThat(rewriter.rewrite(query, Sort.unsorted())).isEqualTo(query);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cf.blockCode", "RND");
        GridColumnFilters filters = GridColumnFilters.from(request, ALIASES);
        try (GridColumnFilterContext ignored = GridColumnFilterContext.open(ALIASES, filters)) {
            String rewritten = rewriter.rewrite(query, Sort.unsorted());
            Assertions.assertThat(rewritten).startsWith("SELECT b FROM Block b WHERE (:search IS NULL) AND (");
            Assertions.assertThat(rewritten).contains("b.blockCode");
        }
        Assertions.assertThat(GridColumnFilterContext.current()).isNull();
    }

    @Test
    @DisplayName("In-memory matcher applies the four operators")
    void matcherAppliesFourOperators() {
        Assertions.assertThat(
                GridColumnFilterMatcher.matches("Burdur Bej", GridFilterOperator.CONTAINS, "bej")).isTrue();
        Assertions.assertThat(
                GridColumnFilterMatcher.matches("Burdur Bej", GridFilterOperator.EQUALS, "burdur bej")).isTrue();
        Assertions.assertThat(
                GridColumnFilterMatcher.matches("Burdur Bej", GridFilterOperator.STARTS_WITH, "bur")).isTrue();
        Assertions.assertThat(
                GridColumnFilterMatcher.matches("Burdur Bej", GridFilterOperator.NOT_CONTAINS, "kirmizi")).isTrue();
        Assertions.assertThat(
                GridColumnFilterMatcher.matches("Burdur Bej", GridFilterOperator.NOT_CONTAINS, "bej")).isFalse();
    }

    @Test
    @DisplayName("Grid links keep other query parameters when clearing one column filter")
    void gridLinksPreserveStateWhenClearingAFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/blocks");
        request.setParameter("stockFilter", "ALL");
        request.setParameter("sort", "blockCode");
        request.setParameter("cf.blockCode", "RND");
        request.setParameter("co.blockCode", "equals");
        request.setParameter("page", "3");
        GridLinks links = new GridLinks(request);
        Assertions.assertThat(links.filtered("blockCode")).isTrue();
        Assertions.assertThat(links.hasColumnFilters()).isTrue();
        Assertions.assertThat(links.clearFilter("blockCode"))
                .contains("stockFilter=ALL")
                .contains("sort=blockCode")
                .contains("page=1")
                .doesNotContain("cf.blockCode")
                .doesNotContain("co.blockCode");
        Assertions.assertThat(links.filterValues("blocks-table", "blockCode"))
                .isEqualTo("/grid-filter-values?grid=blocks-table&column=blockCode");
    }

    @Test
    @DisplayName("Catalog only allows whitelisted keys for a grid")
    void catalogWhitelistsKnownKeys() {
        Assertions.assertThat(GridFilterCatalog.allows("blocks-table", "blockCode")).isTrue();
        Assertions.assertThat(GridFilterCatalog.allows("blocks-table", "notAColumn")).isFalse();
        Assertions.assertThat(GridFilterCatalog.allows("reports-table", "id")).isFalse();
    }

    @Test
    @DisplayName("Date and number filters validate input and render typed JPQL")
    void dateAndNumberFiltersValidateAndRenderJpql() {
        Map<String, String> aliases = Map.of(
                "extractionDate", "extractionDate",
                "totalCost", "totalCost"
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("cf.extractionDate", "2026-11-01");
        request.setParameter("ct.extractionDate", "2026-11-02");
        request.setParameter("co.extractionDate", "between");
        request.setParameter("cf.totalCost", "10");
        request.setParameter("ct.totalCost", "20");
        request.setParameter("co.totalCost", "between");
        GridColumnFilters filters = GridColumnFilters.from(request, aliases, Block.class);
        Assertions.assertThat(filters.getMessages()).isEmpty();
        String sql = GridColumnFilterJpql.predicate("b", aliases, filters, Block.class);
        Assertions.assertThat(sql).contains("b.extractionDate >= cast('2026-11-01' as date)");
        Assertions.assertThat(sql).contains("b.extractionDate <= cast('2026-11-02' as date)");
        Assertions.assertThat(sql).contains("b.totalCost >= 10");
        Assertions.assertThat(sql).contains("b.totalCost <= 20");
        Assertions.assertThat(sql).doesNotContain("LOWER(CAST(b.extractionDate");
        Assertions.assertThat(sql).doesNotContain("LOWER(CAST(b.totalCost");
    }

    @Test
    @DisplayName("Invalid dates are ignored with feedback and from>to is rejected")
    void invalidAndInvertedDateRangesAreRejected() {
        Map<String, String> aliases = Map.of("extractionDate", "extractionDate");
        MockHttpServletRequest invalid = new MockHttpServletRequest();
        invalid.setParameter("cf.extractionDate", "32.13.2026");
        invalid.setParameter("co.extractionDate", "equals");
        GridColumnFilters ignored = GridColumnFilters.from(invalid, aliases, Block.class);
        Assertions.assertThat(ignored.isEmpty()).isTrue();
        Assertions.assertThat(ignored.getMessages()).anyMatch(message -> message.contains("Geçersiz tarih"));

        MockHttpServletRequest inverted = new MockHttpServletRequest();
        inverted.setParameter("cf.extractionDate", "2026-11-10");
        inverted.setParameter("ct.extractionDate", "2026-11-01");
        inverted.setParameter("co.extractionDate", "between");
        GridColumnFilters rejected = GridColumnFilters.from(inverted, aliases, Block.class);
        Assertions.assertThat(rejected.isEmpty()).isTrue();
        Assertions.assertThat(rejected.getMessages())
                .contains("Başlangıç tarihi bitiş tarihinden sonra olamaz.");
    }

    @Test
    @DisplayName("In-memory matcher includes date and number range boundaries")
    void matcherIncludesBetweenBoundaries() {
        GridColumnFilter dates = new GridColumnFilter(
                "extractionDate", GridFilterOperator.BETWEEN, "2026-11-01", "2026-11-02");
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("2026-11-01", dates)).isTrue();
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("2026-11-02", dates)).isTrue();
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("2026-11-03", dates)).isFalse();
        GridColumnFilter numbers = new GridColumnFilter(
                "totalCost", GridFilterOperator.BETWEEN, "10", "20");
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("10", numbers)).isTrue();
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("20", numbers)).isTrue();
        Assertions.assertThat(GridColumnFilterMatcher.matchesValue("20.01", numbers)).isFalse();
    }

    @Test
    @DisplayName("Opening a sort-alias context expands catalog keys such as extractionDate")
    void contextExpandsCatalogAliases() {
        Assertions.assertThat(GridFilterCatalog.expandAliases(GridPages.BLOCK_SORTS))
                .containsKeys("extractionDate", "locationName", "totalCost", "blockCode");
        Assertions.assertThat(GridFilterCatalog.kind("blocks-table", "extractionDate"))
                .isEqualTo(GridFilterKind.DATE);
        Assertions.assertThat(GridFilterCatalog.kind("blocks-table", "totalCost"))
                .isEqualTo(GridFilterKind.NUMBER);
        Assertions.assertThat(GridFilterCatalog.kind("blocks-table", "blockCode"))
                .isEqualTo(GridFilterKind.TEXT);
        try (GridColumnFilterContext context = GridColumnFilterContext.open(GridPages.BLOCK_SORTS)) {
            Assertions.assertThat(context.getAliases()).containsKey("extractionDate");
        }
    }

    @Test
    @DisplayName("Grid links clear the range end parameter with the column filter")
    void gridLinksClearRangeEnd() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/blocks");
        request.setParameter("cf.extractionDate", "2026-11-01");
        request.setParameter("ct.extractionDate", "2026-11-02");
        request.setParameter("co.extractionDate", "between");
        GridLinks links = new GridLinks(request);
        Assertions.assertThat(links.filtered("extractionDate")).isTrue();
        Assertions.assertThat(links.operator("extractionDate", "date")).isEqualTo("between");
        Assertions.assertThat(links.toValue("extractionDate")).isEqualTo("2026-11-02");
        Assertions.assertThat(links.clearFilter("extractionDate"))
                .doesNotContain("cf.extractionDate")
                .doesNotContain("ct.extractionDate")
                .doesNotContain("co.extractionDate");
    }
}
