package com.ozerler.marble.dto;

import java.util.Collections;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
