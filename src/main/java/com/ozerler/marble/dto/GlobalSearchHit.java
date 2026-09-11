package com.ozerler.marble.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchHit {

    private String type;
    private String typeLabel;
    private String title;
    private String subtitle;
    private String url;
    private String icon;
    private int score;
}
