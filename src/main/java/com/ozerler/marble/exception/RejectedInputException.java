package com.ozerler.marble.exception;

import java.util.Arrays;
import java.util.List;

/**
 * A business-rule rejection the user can fix. {@code fieldNames} are HTML {@code name} values
 * for the inputs that should be highlighted. Leave the list empty when no single input is known.
 */
public class RejectedInputException extends IllegalArgumentException {

    private final List<String> fieldNames;

    public RejectedInputException(String message, String... fieldNames) {
        super(message);
        if (fieldNames == null || fieldNames.length == 0) {
            this.fieldNames = List.of();
            return;
        }
        this.fieldNames = Arrays.stream(fieldNames)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    public List<String> fieldNames() {
        return fieldNames;
    }
}
