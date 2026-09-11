package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabulatorResponse<T> {
    private int last_page;
    private long total;
    private List<T> data;

    public static <T> TabulatorResponse<T> of(List<T> data, int totalPages, long totalElements) {
        return TabulatorResponse.<T>builder()
                .data(data != null ? data : Collections.emptyList())
                .last_page(totalPages > 0 ? totalPages : 1)
                .total(totalElements)
                .build();
    }
}
