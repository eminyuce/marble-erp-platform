package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

class LearningCenterControllerTest {

    @Test
    @DisplayName("GET /ogrenme-merkezi returns learning/index view with expected attributes")
    void indexReturnsLearningView() {
        LearningCenterController controller = new LearningCenterController();
        Model model = new ExtendedModelMap();

        String viewName = controller.index(model);

        assertThat(viewName).isEqualTo("learning/index");
        assertThat(model.getAttribute("pageTitle")).isEqualTo("10 Adımda Öğrenme Merkezi");
        assertThat(model.getAttribute("helpPageKey")).isEqualTo("learning-center");
        assertThat(model.getAttribute("currentSection")).isEqualTo("learning-center");
    }
}
