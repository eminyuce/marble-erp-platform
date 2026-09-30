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
        assertThat(controller.ocakUretimYeni()).isEqualTo("redirect:/blocks/create");
        assertThat(controller.ocakStok()).isEqualTo("redirect:/blocks");
        assertThat(controller.giderlerYeni()).isEqualTo("redirect:/expenses/create");
        assertThat(controller.fabrikaKesim()).isEqualTo("redirect:/production/create");
        assertThat(controller.fabrikaCila()).isEqualTo("redirect:/production/polish");
        assertThat(controller.fabrikaEbatlama()).isEqualTo("redirect:/production/pallets");
        assertThat(controller.atolyeIsEmri()).isEqualTo("redirect:/workshop/create");
        assertThat(controller.santiyePlanlama()).isEqualTo("redirect:/projects/create");
        assertThat(controller.santiyeMontaj()).isEqualTo("redirect:/projects");
        assertThat(controller.maliyetAnalizi()).isEqualTo("redirect:/costs");
    }
}
