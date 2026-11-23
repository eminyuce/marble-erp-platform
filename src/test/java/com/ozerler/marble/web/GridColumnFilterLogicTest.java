package com.ozerler.marble.web;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockHttpServletRequest;

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
}
