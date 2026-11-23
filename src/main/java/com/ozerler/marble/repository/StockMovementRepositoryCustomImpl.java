package com.ozerler.marble.repository;

import com.ozerler.marble.model.StockMovement;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.StockMovementType;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockMovementRepositoryCustomImpl implements StockMovementRepositoryCustom {

    private final EntityManager entityManager;

    public StockMovementRepositoryCustomImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<StockMovement> searchMovements(StockMovementType movementType,
                                               BusinessUnit sourceDept,
                                               BusinessUnit targetDept,
                                               Long customerId,
                                               LocalDateTime fromDate,
                                               LocalDateTime toDate,
                                               Pageable pageable) {
        MovementFilter filter = new MovementFilter(movementType, sourceDept, targetDept, customerId, fromDate, toDate);
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<StockMovement> rows = builder.createQuery(StockMovement.class);
        Root<StockMovement> root = rows.from(StockMovement.class);
        rows.select(root).where(predicates(builder, root, filter));
        rows.orderBy(orders(builder, root, pageable.getSort()));

        TypedQuery<StockMovement> query = entityManager.createQuery(rows);
        query.setHint("jakarta.persistence.fetchgraph", movementGraph());
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<StockMovement> content = query.getResultList();

        CriteriaQuery<Long> count = builder.createQuery(Long.class);
        Root<StockMovement> countRoot = count.from(StockMovement.class);
        count.select(builder.count(countRoot)).where(predicates(builder, countRoot, filter));
        long total = entityManager.createQuery(count).getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private EntityGraph<StockMovement> movementGraph() {
        EntityGraph<StockMovement> graph = entityManager.createEntityGraph(StockMovement.class);
        graph.addAttributeNodes("block", "slab", "stockItem", "fromLocation", "toLocation", "customer");
        return graph;
    }

    private static Predicate[] predicates(CriteriaBuilder builder, Root<StockMovement> root, MovementFilter filter) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.movementType() != null) {
            predicates.add(builder.equal(root.get("movementType"), filter.movementType()));
        }
        if (filter.sourceDept() != null) {
            predicates.add(builder.equal(root.get("sourceDepartment"), filter.sourceDept()));
        }
        if (filter.targetDept() != null) {
            predicates.add(builder.equal(root.get("targetDepartment"), filter.targetDept()));
        }
        if (filter.customerId() != null) {
            predicates.add(builder.equal(root.get("customer").get("id"), filter.customerId()));
        }
        if (filter.fromDate() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("movementDate"), filter.fromDate()));
        }
        if (filter.toDate() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("movementDate"), filter.toDate()));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private static List<Order> orders(CriteriaBuilder builder, Root<StockMovement> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        if (sort != null) {
            for (Sort.Order order : sort) {
                orders.add(order.isAscending()
                        ? builder.asc(root.get(order.getProperty()))
                        : builder.desc(root.get(order.getProperty())));
            }
        }
        if (orders.isEmpty()) {
            orders.add(builder.desc(root.get("movementDate")));
            orders.add(builder.desc(root.get("id")));
        }
        return orders;
    }

    private record MovementFilter(StockMovementType movementType,
                                  BusinessUnit sourceDept,
                                  BusinessUnit targetDept,
                                  Long customerId,
                                  LocalDateTime fromDate,
                                  LocalDateTime toDate) {
    }
}
