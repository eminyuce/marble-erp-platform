package com.ozerler.marble.controller;

import com.ozerler.marble.dto.HelpFeedbackRequest;
import com.ozerler.marble.dto.HelpFeedbackResponse;
import com.ozerler.marble.dto.HelpPageDto;
import com.ozerler.marble.service.HelpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contextual help API used by the shared slide-over panel.
 * <p>
 * GET  /api/help/{pageKey}
 * POST /api/help/{pageKey}/feedback  { "helpful": true }
 */
@RestController
@RequestMapping("/api/help")
@RequiredArgsConstructor
public class HelpController {

    private final HelpService helpService;

    @GetMapping("/{pageKey}")
    public HelpPageDto getHelp(@PathVariable("pageKey") String pageKey) {
        return helpService.getHelpPage(pageKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Help not found"));
    }

    @PostMapping("/{pageKey}/feedback")
    public ResponseEntity<HelpFeedbackResponse> submitFeedback(
            @PathVariable("pageKey") String pageKey,
            @Valid @RequestBody HelpFeedbackRequest request) {
        try {
            return ResponseEntity.ok(helpService.recordFeedback(pageKey, Boolean.TRUE.equals(request.getHelpful())));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}
