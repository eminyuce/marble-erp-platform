package com.ozerler.marble.dto;

import java.util.Objects;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response wrapper formatted for Tabulator 6 remote pagination.
 *
 * @param <T> data item type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabulatorResponse<T> {

    @JsonProperty("last_page")
    @JsonAlias("lastPage")
    private int lastPage;

    @JsonProperty("total")
    @JsonAlias("total")
    private long total;

    @JsonProperty("data")
    @JsonAlias("data")
    private List<T> data;

    @JsonProperty("meta")
    @JsonAlias("meta")
    private Map<String, Object> meta;

    public static <T> TabulatorResponse<T> of(List<T> data, int totalPages, long totalElements) {
        return of(data, totalPages, totalElements, null);
    }

    public static <T> TabulatorResponse<T> of(List<T> data, int totalPages, long totalElements,
                                              Map<String, Object> meta) {
        int totalPagesValue;
        if (totalPages > 0) {
            totalPagesValue = totalPages;
        } else {
            totalPagesValue = 1;
        }
        return TabulatorResponse.<T>builder()
                .data(Objects.requireNonNullElse(data, Collections.emptyList()))
                .lastPage(totalPagesValue)
                .total(totalElements)
                .meta(meta)
                .build();
    }
}
