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
public class GlobalSearchResponse {

    private String query;
    private int total;
    private List<GlobalSearchHit> results;

    public static GlobalSearchResponse empty(String query) {
        return GlobalSearchResponse.builder()
                .query(query)
                .total(0)
                .results(Collections.emptyList())
                .build();
    }
}
