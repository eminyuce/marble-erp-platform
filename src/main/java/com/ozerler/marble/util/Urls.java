package com.ozerler.marble.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class Urls {

    private Urls() {
    }

    public static String encode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
