package com.ozerler.marble.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Static bridge and helper for retrieving localized messages from Spring MessageSource,
 * falling back to ResourceBundle when outside the Spring ApplicationContext.
 */
@Component
public class MessageUtils {

    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("tr");
    private static MessageSource messageSource;

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    public static String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        if (locale == null || !"tr".equalsIgnoreCase(locale.getLanguage())) {
            locale = DEFAULT_LOCALE;
        }
        return getMessage(code, locale, args);
    }

    public static String getMessage(String code, Locale locale, Object... args) {
        Locale targetLocale = (locale != null && "tr".equalsIgnoreCase(locale.getLanguage())) ? locale : DEFAULT_LOCALE;
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, targetLocale);
            } catch (Exception ignored) {
                // fall back to ResourceBundle
            }
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", targetLocale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
            if (bundle.containsKey(code)) {
                String pattern = bundle.getString(code);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
            // fall back to default ResourceBundle lookup
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", targetLocale);
            if (bundle.containsKey(code)) {
                String pattern = bundle.getString(code);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return code;
    }
}
