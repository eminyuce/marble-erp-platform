package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Criteria predicates equivalent to {@link GridColumnFilterJpql}, used by Specification-based lists.
 */
public final class GridColumnFilterSpecifications {

    private GridColumnFilterSpecifications() {
    }

    public static <T> Specification<T> of(Map<String, String> aliases, GridColumnFilters filters) {
        return (root, query, builder) -> {
            if (filters == null || filters.isEmpty()) {
                return builder.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            for (GridColumnFilter filter : filters.getItems()) {
                String relative;
                if (aliases == null) {
                    relative = null;
                } else {
                    relative = aliases.get(filter.getKey());
                }
                if (relative == null || !GridColumnFilterJpql.isSafePath(relative)) {
                    continue;
                }
                Path<?> path = resolve(root, relative);
                if (path != null) {
                    predicates.add(predicate(builder, path, filter));
                }
            }
            if (predicates.isEmpty()) {
                return builder.conjunction();
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Path<?> resolve(From<?, ?> root, String relative) {
        String[] parts = relative.split("\\.");
        From<?, ?> current = root;
        Path<?> path = root;
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (index < parts.length - 1) {
                current = current.join(part, JoinType.LEFT);
                path = current;
            } else {
                path = path.get(part);
            }
        }
        return path;
    }

    private static Predicate predicate(CriteriaBuilder builder, Path<?> path, GridColumnFilter filter) {
        var text = builder.lower(path.as(String.class));
        String expected = filter.getValue().toLowerCase(Locale.ROOT);
        GridFilterOperator operator = filter.getOperator();
        if (operator == GridFilterOperator.EQUALS) {
            return builder.equal(text, expected);
        }
        String pattern = GridLikeEscapes.likePattern(filter.getValue(), operator).toLowerCase(Locale.ROOT);
        Predicate like = builder.like(text, pattern, GridLikeEscapes.ESCAPE_CHAR);
        if (operator == GridFilterOperator.NOT_CONTAINS) {
            return builder.or(builder.isNull(path), builder.not(like));
        }
        return like;
    }
}
