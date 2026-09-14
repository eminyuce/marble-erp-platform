package com.ozerler.marble.service;

import com.ozerler.marble.dto.HelpFeedbackResponse;
import com.ozerler.marble.dto.HelpPageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HelpService {

    private static final Pattern PAGE_KEY_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{0,63}$");

    private final HelpContentCatalog helpContentCatalog;
    private final ConcurrentHashMap<String, FeedbackCounts> feedbackByPage = new ConcurrentHashMap<>();

    public Optional<HelpPageDto> getHelpPage(String pageKey) {
        String normalized = normalizePageKey(pageKey);
        if (normalized == null) {
            return Optional.empty();
        }
        return helpContentCatalog.findByPageKey(normalized);
    }

    public Optional<String> resolvePageKey(String requestUri) {
        return helpContentCatalog.resolvePageKey(requestUri);
    }

    public HelpFeedbackResponse recordFeedback(String pageKey, boolean helpful) {
        String normalized = normalizePageKey(pageKey);
        if (normalized == null) {
            throw new IllegalArgumentException("Unknown help page");
        }
        if (helpContentCatalog.findByPageKey(normalized).isEmpty()) {
            throw new IllegalArgumentException("Unknown help page: " + normalized);
        }
        FeedbackCounts counts = feedbackByPage.computeIfAbsent(normalized, key -> new FeedbackCounts());
        if (helpful) {
            counts.helpful.increment();
        } else {
            counts.notHelpful.increment();
        }
        log.info("Help feedback recorded pageKey={} helpful={} totals={}/{}",
                normalized, helpful, counts.helpful.sum(), counts.notHelpful.sum());
        return HelpFeedbackResponse.builder()
                .recorded(true)
                .helpful(helpful)
                .pageKey(normalized)
                .build();
    }

    static String normalizePageKey(String pageKey) {
        if (pageKey == null || pageKey.isBlank()) {
            return null;
        }
        String normalized = pageKey.trim().toLowerCase();
        if (!PAGE_KEY_PATTERN.matcher(normalized).matches()) {
            return null;
        }
        return normalized;
    }

    private static final class FeedbackCounts {
        private final LongAdder helpful = new LongAdder();
        private final LongAdder notHelpful = new LongAdder();
    }
}
