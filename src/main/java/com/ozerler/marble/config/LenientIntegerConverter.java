package com.ozerler.marble.config;

import com.ozerler.marble.util.Ints;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Converts Tabulator query parameters such as {@code page=undefined} into
 * {@code null} so controller {@code defaultValue} bindings still apply.
 */
@Component
public class LenientIntegerConverter implements Converter<String, Integer> {

    @Override
    public Integer convert(String source) {
        return Ints.parseLenientOrNull(source);
    }
}
