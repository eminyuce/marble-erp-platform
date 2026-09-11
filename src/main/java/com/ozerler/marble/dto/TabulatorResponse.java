package com.ozerler.marble.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

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
    private int last_page;

    @JsonProperty("total")
    @JsonAlias("total")
    private long total;

    @JsonProperty("data")
    @JsonAlias("data")
    private List<T> data;

    public static <T> TabulatorResponse<T> of(List<T> data, int totalPages, long totalElements) {
        return TabulatorResponse.<T>builder()
                .data(data != null ? data : Collections.emptyList())
                .last_page(totalPages > 0 ? totalPages : 1)
                .total(totalElements)
                .build();
    }
}
