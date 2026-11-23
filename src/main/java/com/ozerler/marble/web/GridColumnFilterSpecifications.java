package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
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
        return of(aliases, filters, GridFilterCatalog.entityTypeForAliases(aliases));
    }

    public static <T> Specification<T> of(Map<String, String> aliases, GridColumnFilters filters,
                                          Class<?> entityType) {
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
                    Class<?> javaType = GridFilterTypes.javaType(entityType, relative);
                    predicates.add(predicate(builder, path, javaType, filter));
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

    private static Predicate predicate(CriteriaBuilder builder, Path<?> path, Class<?> javaType,
                                       GridColumnFilter filter) {
        GridFilterKind kind = GridFilterTypes.kindOf(javaType);
        if (kind == GridFilterKind.DATE) {
            return datePredicate(builder, path, javaType, filter);
        }
        if (kind == GridFilterKind.NUMBER) {
            return numberPredicate(builder, path, filter);
        }
        return textPredicate(builder, path, filter);
    }

    private static Predicate textPredicate(CriteriaBuilder builder, Path<?> path, GridColumnFilter filter) {
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

    @SuppressWarnings("unchecked")
    private static Predicate datePredicate(CriteriaBuilder builder, Path<?> path, Class<?> javaType,
                                           GridColumnFilter filter) {
        LocalDate from = GridFilterTypes.parseDate(filter.getValue());
        if (from == null) {
            return builder.conjunction();
        }
        GridFilterOperator operator = filter.getOperator();
        if (LocalDate.class.equals(javaType)) {
            Path<LocalDate> typed = (Path<LocalDate>) path;
            if (operator == GridFilterOperator.BEFORE) {
                return builder.lessThan(typed, from);
            }
            if (operator == GridFilterOperator.AFTER) {
                return builder.greaterThan(typed, from);
            }
            if (operator == GridFilterOperator.BETWEEN) {
                LocalDate to = GridFilterTypes.parseDate(filter.getToValue());
                if (to == null) {
                    return builder.conjunction();
                }
                return builder.between(typed, from, to);
            }
            return builder.equal(typed, from);
        }
        if (LocalDateTime.class.equals(javaType)) {
            return dateTimeBounds(builder, (Path<LocalDateTime>) path, operator,
                    filter, GridFilterTypes.startOfDay(from),
                    GridFilterTypes.startOfNextDay(from),
                    to -> GridFilterTypes.startOfNextDay(to));
        }
        if (Instant.class.equals(javaType)) {
            return dateTimeBounds(builder, (Path<Instant>) path, operator, filter,
                    GridFilterTypes.startOfDayInstant(from),
                    GridFilterTypes.startOfNextDayInstant(from),
                    GridFilterTypes::startOfNextDayInstant);
        }
        if (OffsetDateTime.class.equals(javaType)) {
            OffsetDateTime start = GridFilterTypes.startOfDayInstant(from).atOffset(java.time.ZoneOffset.UTC);
            OffsetDateTime next = GridFilterTypes.startOfNextDayInstant(from).atOffset(java.time.ZoneOffset.UTC);
            return dateTimeBounds(builder, (Path<OffsetDateTime>) path, operator, filter, start, next,
                    date -> GridFilterTypes.startOfNextDayInstant(date).atOffset(java.time.ZoneOffset.UTC));
        }
        if (ZonedDateTime.class.equals(javaType)) {
            ZonedDateTime start = GridFilterTypes.startOfDayInstant(from).atZone(java.time.ZoneOffset.UTC);
            ZonedDateTime next = GridFilterTypes.startOfNextDayInstant(from).atZone(java.time.ZoneOffset.UTC);
            return dateTimeBounds(builder, (Path<ZonedDateTime>) path, operator, filter, start, next,
                    date -> GridFilterTypes.startOfNextDayInstant(date).atZone(java.time.ZoneOffset.UTC));
        }
        if (Date.class.equals(javaType)) {
            Date start = Date.from(GridFilterTypes.startOfDayInstant(from));
            Date next = Date.from(GridFilterTypes.startOfNextDayInstant(from));
            return dateTimeBounds(builder, (Path<Date>) path, operator, filter, start, next,
                    date -> Date.from(GridFilterTypes.startOfNextDayInstant(date)));
        }
        return builder.conjunction();
    }

    private static <T extends Comparable<? super T>> Predicate dateTimeBounds(
            CriteriaBuilder builder, Path<T> path, GridFilterOperator operator,
            GridColumnFilter filter, T start, T next,
            java.util.function.Function<LocalDate, T> nextAfterTo) {
        if (operator == GridFilterOperator.BEFORE) {
            return builder.lessThan(path, start);
        }
        if (operator == GridFilterOperator.AFTER) {
            return builder.greaterThanOrEqualTo(path, next);
        }
        if (operator == GridFilterOperator.BETWEEN) {
            LocalDate to = GridFilterTypes.parseDate(filter.getToValue());
            if (to == null) {
                return builder.conjunction();
            }
            return builder.and(
                    builder.greaterThanOrEqualTo(path, start),
                    builder.lessThan(path, nextAfterTo.apply(to)));
        }
        return builder.and(builder.greaterThanOrEqualTo(path, start), builder.lessThan(path, next));
    }

    @SuppressWarnings("unchecked")
    private static Predicate numberPredicate(CriteriaBuilder builder, Path<?> path, GridColumnFilter filter) {
        BigDecimal from = GridFilterTypes.parseNumber(filter.getValue());
        if (from == null) {
            return builder.conjunction();
        }
        Expression<BigDecimal> typed = path.as(BigDecimal.class);
        if (filter.getOperator() == GridFilterOperator.BETWEEN) {
            BigDecimal to = GridFilterTypes.parseNumber(filter.getToValue());
            if (to == null) {
                return builder.conjunction();
            }
            return builder.between(typed, from, to);
        }
        return builder.equal(typed, from);
    }
}
