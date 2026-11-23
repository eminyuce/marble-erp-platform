package com.ozerler.marble.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ozerler.marble.web.GridColumnFilterJpql;
import com.ozerler.marble.web.GridColumnFilterSpecifications;
import com.ozerler.marble.web.GridFilterCatalog;
import com.ozerler.marble.web.GridFilterValue;

/**
 * Loads distinct values for a whitelisted grid column, capped for the header-filter list.
 */
@Service
public class GridColumnFilterValuesService {

    public static final int LIMIT = 100;

    private final EntityManager entityManager;

    public GridColumnFilterValuesService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<GridFilterValue> values(String tableId, String columnKey) {
        if (!GridFilterCatalog.allows(tableId, columnKey)) {
            return List.of();
        }
        Class<?> entityType = GridFilterCatalog.entityType(tableId);
        String relative = GridFilterCatalog.aliases(tableId).get(columnKey);
        if (entityType == null || relative == null || !GridColumnFilterJpql.isSafePath(relative)) {
            return List.of();
        }
        List<String> raw = load(entityType, relative);
        return format(entityType, relative, raw);
    }

    private List<String> load(Class<?> entityType, String relative) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = builder.createQuery(String.class);
        Root<?> root = query.from(entityType);
        Path<?> path = GridColumnFilterSpecifications.resolve(root, relative);
        query.select(path.as(String.class)).distinct(true);
        query.where(builder.isNotNull(path));
        query.orderBy(builder.asc(path));
        return entityManager.createQuery(query).setMaxResults(LIMIT).getResultList();
    }

    private List<GridFilterValue> format(Class<?> entityType, String relative, List<String> raw) {
        Class<? extends Enum<?>> enumType = enumType(entityType, relative);
        List<GridFilterValue> values = new ArrayList<>();
        for (String item : raw) {
            if (item == null || item.isBlank()) {
                continue;
            }
            values.add(new GridFilterValue(item, labelFor(enumType, item)));
        }
        return List.copyOf(values);
    }

    private static Class<? extends Enum<?>> enumType(Class<?> entityType, String relative) {
        try {
            Class<?> current = entityType;
            String[] parts = relative.split("\\.");
            java.lang.reflect.Field field = null;
            for (String part : parts) {
                field = declared(current, part);
                current = field.getType();
            }
            if (field != null && current.isEnum()) {
                @SuppressWarnings("unchecked")
                Class<? extends Enum<?>> type = (Class<? extends Enum<?>>) current;
                return type;
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static java.lang.reflect.Field declared(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static String labelFor(Class<? extends Enum<?>> enumType, String name) {
        if (enumType == null) {
            return name;
        }
        try {
            Enum<?> constant = Enum.valueOf(enumType.asSubclass(Enum.class), name);
            Method label = constant.getClass().getMethod("getLabel");
            Object value = label.invoke(constant);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        } catch (RuntimeException | ReflectiveOperationException ignored) {
            return name;
        }
        return name;
    }
}
