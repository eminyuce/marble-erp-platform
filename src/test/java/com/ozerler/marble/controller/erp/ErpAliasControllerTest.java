package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ErpAliasControllerTest {

    @Test
    @DisplayName("canonical Turkish URLs redirect to existing module paths")
    void aliasesRedirectToLegacyPaths() {
        ErpAliasController controller = new ErpAliasController();
        assertThat(controller.quarry()).isEqualTo("redirect:/blocks");
        assertThat(controller.factory()).isEqualTo("redirect:/production");
        assertThat(controller.sites()).isEqualTo("redirect:/projects");
        assertThat(controller.costAnalysis()).isEqualTo("redirect:/costs");
    }
}
