package com.ozerler.marble.web;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.QueryRewriter;

/**
 * Appends the active header-filter predicate to list {@code @Query} JPQL when a context is open.
 */
public class GridColumnFilterRewriter implements QueryRewriter {

    @Override
    public String rewrite(String query, Sort sort) {
        GridColumnFilterContext context = GridColumnFilterContext.current();
        if (context == null || query == null || query.isBlank()) {
            return query;
        }
        String alias = rootAlias(query);
        if (alias == null) {
            return query;
        }
        String predicate = GridColumnFilterJpql.predicate(alias, context.getAliases(), context.getFilters());
        if (predicate.isBlank()) {
            return query;
        }
        return appendPredicate(query, predicate);
    }

    static String rootAlias(String query) {
        if (query == null) {
            return null;
        }
        String upper = query.toUpperCase();
        int from = lastTopLevel(upper, " FROM ");
        if (from < 0) {
            return null;
        }
        String tail = query.substring(from + 6).trim();
        String[] tokens = tail.split("\\s+");
        if (tokens.length < 2) {
            return null;
        }
        int index = 1;
        if ("AS".equalsIgnoreCase(tokens[1]) && tokens.length >= 3) {
            index = 2;
        }
        String alias = tokens[index].replace(",", "");
        if (GridColumnFilterJpql.isSafePath(alias) && alias.indexOf('.') < 0) {
            return alias;
        }
        return null;
    }

    static String appendPredicate(String query, String predicate) {
        String trimmed = query.stripTrailing();
        int cut = trimmed.length();
        int orderBy = lastTopLevel(trimmed.toUpperCase(), " ORDER BY ");
        int groupBy = lastTopLevel(trimmed.toUpperCase(), " GROUP BY ");
        if (orderBy >= 0) {
            cut = Math.min(cut, orderBy);
        }
        if (groupBy >= 0) {
            cut = Math.min(cut, groupBy);
        }
        String head = trimmed.substring(0, cut);
        String tail = trimmed.substring(cut);
        String combiner;
        if (lastTopLevel(head.toUpperCase(), " WHERE ") >= 0) {
            combiner = " AND ";
        } else {
            combiner = " WHERE ";
        }
        return head + combiner + predicate + tail;
    }

    static int lastTopLevel(String upperQuery, String keyword) {
        int found = -1;
        int depth = 0;
        boolean quote = false;
        int keywordLength = keyword.length();
        for (int index = 0; index <= upperQuery.length() - keywordLength; index++) {
            char character = upperQuery.charAt(index);
            if (character == '\'') {
                quote = !quote;
            } else if (!quote && character == '(') {
                depth++;
            } else if (!quote && character == ')' && depth > 0) {
                depth--;
            } else if (!quote && depth == 0 && upperQuery.startsWith(keyword, index)) {
                found = index;
            }
        }
        return found;
    }
}
