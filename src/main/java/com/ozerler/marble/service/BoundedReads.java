package com.ozerler.marble.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Reads a full table as pages so no caller issues an unbounded {@code findAll()}.
 * The assembled list is still the complete set; each SQL statement is limited.
 */
public final class BoundedReads {

    public static final int PAGE_SIZE = 200;

    private BoundedReads() {
    }

    public static <T> List<T> all(JpaRepository<T, ?> repository) {
        return all(repository, Sort.unsorted());
    }

    public static <T> List<T> all(JpaRepository<T, ?> repository, Sort sort) {
        List<T> rows = new ArrayList<>();
        Pageable page = PageRequest.of(0, PAGE_SIZE, sort);
        while (true) {
            Page<T> slice = repository.findAll(page);
            rows.addAll(slice.getContent());
            if (!slice.hasNext()) {
                return rows;
            }
            page = slice.nextPageable();
        }
    }
}
