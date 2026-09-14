package com.ozerler.marble.util;

import com.ozerler.marble.common.Constants;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.function.Function;

/**
 * Builds Tabulator remote-pagination {@link Pageable} values and retries when a
 * grid column name is not a persistable entity property.
 */
public final class GridPages {

    private GridPages() {
    }

    public static Pageable of(int page, int size, String sortField, String sortDir) {
        return of(page, size, sortField, sortDir, "createdDate");
    }

    public static Pageable of(int page, int size, String sortField, String sortDir, String defaultProperty) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = size > 0 ? size : Constants.DEFAULT_PAGE_SIZE;
        return PageRequest.of(pageIndex, pageSize, sort(sortField, sortDir, defaultProperty));
    }

    public static Sort sort(String sortField, String sortDir, String defaultProperty) {
        String fallback = (defaultProperty == null || defaultProperty.isBlank()) ? "createdDate" : defaultProperty;
        String property = fallback;
        if (sortField != null && !sortField.isBlank() && !"createdAt".equalsIgnoreCase(sortField)) {
            property = toEntityProperty(sortField);
            if (property.isBlank()) {
                property = fallback;
            }
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    public static <T> Page<T> execute(int page, int size, String sortField, String sortDir,
                                      Function<Pageable, Page<T>> query) {
        return execute(of(page, size, sortField, sortDir), query);
    }

    public static <T> Page<T> execute(Pageable pageable, Function<Pageable, Page<T>> query) {
        try {
            return query.apply(pageable);
        } catch (RuntimeException ex) {
            if (!isUnsafeSort(ex) || isDefaultCreatedDateSort(pageable)) {
                throw ex;
            }
            Sort.Direction direction = pageable.getSort().stream()
                    .findFirst()
                    .map(Sort.Order::getDirection)
                    .orElse(Sort.Direction.DESC);
            Pageable fallback = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(direction, "createdDate"));
            return query.apply(fallback);
        }
    }

    static String toEntityProperty(String sortField) {
        if (sortField.indexOf('_') < 0) {
            return sortField;
        }
        StringBuilder property = new StringBuilder(sortField.length());
        boolean upperNext = false;
        for (int i = 0; i < sortField.length(); i++) {
            char character = sortField.charAt(i);
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                property.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                property.append(character);
            }
        }
        return property.toString();
    }

    static boolean isUnsafeSort(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof PropertyReferenceException
                    || current instanceof InvalidDataAccessApiUsageException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isDefaultCreatedDateSort(Pageable pageable) {
        return pageable.getSort().stream().allMatch(order -> "createdDate".equals(order.getProperty()));
    }
}
