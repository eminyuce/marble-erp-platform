package com.ozerler.marble.exception;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ozerler.marble.web.FormDraft;

class UserFacingExceptionHandlerTest {

    private UserFacingExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ExtendedModelMap model;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        handler = new UserFacingExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/production/polish");
        request.setMethod("POST");
        request.setServerName("localhost");
        response = new MockHttpServletResponse();
        model = new ExtendedModelMap();
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    @DisplayName("HTML form posts return to the same page with the exception sentence")
    void htmlPostRedirectsWithReadableMessage() {
        request.addHeader("Referer", "http://localhost/production/polish");
        request.setParameter("operatorName", "Ayşe");
        request.setParameter("processType", "SLAB_POLISHING");
        request.setParameter("inputM2", "12.5");
        request.setParameter("_csrf", "secret-token");
        request.setParameter("password", "hidden");
        RejectedInputException ex = new RejectedInputException(
                "Bu iş emri ST kesimden geldi. Yalnızca Bant Silim (Dar Bant Cila) seçebilirsiniz.",
                "processType");

        Object view = handler.handleIllegalArgument(ex, request, response, null, model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/production/polish");
        Assertions.assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Bu iş emri ST kesimden geldi. Yalnızca Bant Silim (Dar Bant Cila) seçebilirsiniz.");
        Assertions.assertThat(redirectAttributes.getFlashAttributes().get(FormDraft.FIELDS))
                .isEqualTo(List.of("processType"));
        @SuppressWarnings("unchecked")
        Map<String, List<String>> preserved =
                (Map<String, List<String>>) redirectAttributes.getFlashAttributes().get(FormDraft.VALUES);
        Assertions.assertThat(preserved.get("operatorName")).containsExactly("Ayşe");
        Assertions.assertThat(preserved.get("processType")).containsExactly("SLAB_POLISHING");
        Assertions.assertThat(preserved.get("inputM2")).containsExactly("12.5");
        Assertions.assertThat(preserved).doesNotContainKey("_csrf");
        Assertions.assertThat(preserved).doesNotContainKey("password");
        Assertions.assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    @Test
    @DisplayName("JSON callers receive 400 and the same sentence, without a redirect")
    void jsonRequestReturnsBadRequest() {
        request.addHeader("Accept", "application/json");

        Object body = handler.handleIllegalArgument(
                new IllegalArgumentException("Blok kesim için hazır değil"),
                request, response, null, model, redirectAttributes);

        Assertions.assertThat(body).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) body;
        Assertions.assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(entity.getBody()).isInstanceOf(org.springframework.http.ProblemDetail.class);
        org.springframework.http.ProblemDetail error = (org.springframework.http.ProblemDetail) entity.getBody();
        Assertions.assertThat(error.getDetail()).isEqualTo("Blok kesim için hazır değil");
        Assertions.assertThat(error.getInstance()).hasToString("/production/polish");
    }

    @Test
    @DisplayName("A foreign referer is ignored so the browser stays inside the application")
    void foreignRefererFallsBackToRequestPath() {
        request.addHeader("Referer", "https://evil.example/phish");

        Object view = handler.handleIllegalArgument(
                new IllegalArgumentException("Makine adı boş olamaz."),
                request, response, null, model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/production/polish");
        Assertions.assertThat(redirectAttributes.getFlashAttributes().get("errorMessage"))
                .isEqualTo("Makine adı boş olamaz.");
    }

    @Test
    @DisplayName("GET failures render the in-app notice instead of redirecting onto themselves")
    void getRendersUserMessageView() {
        request.setMethod("GET");

        Object view = handler.handleIllegalArgument(
                new IllegalArgumentException("Fabrika iş emri bulunamadı: 4"),
                request, response, null, model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo(UserFacingExceptionHandler.ERROR_VIEW);
        Assertions.assertThat(response.getStatus()).isEqualTo(400);
        Assertions.assertThat(model.getAttribute("errorMessage")).isEqualTo("Fabrika iş emri bulunamadı: 4");
        Assertions.assertThat(redirectAttributes.getFlashAttributes()).isEmpty();
    }
}
