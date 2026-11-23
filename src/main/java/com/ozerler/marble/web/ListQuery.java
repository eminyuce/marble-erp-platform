package com.ozerler.marble.web;

import lombok.Getter;
import lombok.Setter;

/**
 * Page, size, search, and sort shared by the server-rendered lists.
 * Spring binds the query string onto this object so each controller does not repeat those parameters.
 */
@Getter
@Setter
public class ListQuery {

    private Integer page;
    private Integer size;
    private String search;
    private String sort;
    private String dir;
    private String sortField;
    private String sortDir;

    public int pageNumber() {
        return GridRequests.page(page);
    }

    public int pageSize() {
        return GridRequests.size(size);
    }

    public String sortKey() {
        return GridRequests.sort(sort, sortField);
    }

    public String sortDirection() {
        return GridRequests.direction(dir, sortDir);
    }
}
