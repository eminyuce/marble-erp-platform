package com.ozerler.marble.web;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;

class FormDraftInterceptorTest {

    private final FormDraftInterceptor interceptor = new FormDraftInterceptor();

    @Test
    @DisplayName("A re-rendered form keeps the posted values and drops the CSRF token")
    void keepsPostedValuesOnTheFormView() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/blocks/create");
        request.setParameter("blockCode", "BLK-1");
        request.setParameter("_csrf", "token");
        ModelAndView modelAndView = new ModelAndView("erp/blocks/form");
        modelAndView.addObject("errorMessage", "Blok kodu zorunludur");

        interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), modelAndView);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> preserved =
                (Map<String, List<String>>) modelAndView.getModel().get(FormDraft.VALUES);
        Assertions.assertThat(preserved.get("blockCode")).containsExactly("BLK-1");
        Assertions.assertThat(preserved).doesNotContainKey("_csrf");
        Assertions.assertThat(modelAndView.getModel().get(FormDraft.ACTION)).isEqualTo("/blocks/create");
    }

    @Test
    @DisplayName("A redirect after an error carries the posted values in the flash")
    void keepsPostedValuesOnRedirect() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/production/polish");
        request.setParameter("operatorName", "Ayşe");
        FlashMap flash = new FlashMap();
        flash.put("errorMessage", "Bu iş emri ST kesimden geldi.");
        request.setAttribute(DispatcherServlet.OUTPUT_FLASH_MAP_ATTRIBUTE, flash);
        ModelAndView modelAndView = new ModelAndView("redirect:/production/polish");

        interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), modelAndView);

        @SuppressWarnings("unchecked")
        Map<String, List<String>> preserved = (Map<String, List<String>>) flash.get(FormDraft.VALUES);
        Assertions.assertThat(preserved.get("operatorName")).containsExactly("Ayşe");
        Assertions.assertThat(flash.get(FormDraft.ACTION)).isEqualTo("/production/polish");
    }

    @Test
    @DisplayName("Successful submits do not keep a form draft")
    void ignoresSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/production/polish");
        request.setParameter("operatorName", "Ayşe");
        ModelAndView modelAndView = new ModelAndView("redirect:/production/polish");
        modelAndView.addObject("successMessage", "Kaydedildi");

        interceptor.postHandle(request, new MockHttpServletResponse(), new Object(), modelAndView);

        Assertions.assertThat(modelAndView.getModel()).doesNotContainKey(FormDraft.VALUES);
    }
}
