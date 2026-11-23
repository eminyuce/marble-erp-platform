package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErpAliasControllerTest {

    @Test
    @DisplayName("canonical Turkish URLs redirect to existing module paths")
    void aliasesRedirectToLegacyPaths() {
        ErpAliasController controller = new ErpAliasController();
        Assertions.assertThat(controller.quarry()).isEqualTo("redirect:/blocks");
        Assertions.assertThat(controller.factory()).isEqualTo("redirect:/production");
        Assertions.assertThat(controller.sites()).isEqualTo("redirect:/projects");
        Assertions.assertThat(controller.costAnalysis()).isEqualTo("redirect:/costs");
        Assertions.assertThat(controller.ocakUretimYeni()).isEqualTo("redirect:/blocks/create");
        Assertions.assertThat(controller.ocakStok()).isEqualTo("redirect:/blocks");
        Assertions.assertThat(controller.giderlerYeni()).isEqualTo("redirect:/expenses/create");
        Assertions.assertThat(controller.fabrikaKesim()).isEqualTo("redirect:/production/create");
        Assertions.assertThat(controller.fabrikaCila()).isEqualTo("redirect:/production/polish");
        Assertions.assertThat(controller.fabrikaEbatlama()).isEqualTo("redirect:/operations/factory/sized");
        Assertions.assertThat(controller.atolyeIsEmri()).isEqualTo("redirect:/workshop/create");
        Assertions.assertThat(controller.santiyePlanlama()).isEqualTo("redirect:/projects/create");
        Assertions.assertThat(controller.santiyeMontaj()).isEqualTo("redirect:/projects");
        Assertions.assertThat(controller.maliyetAnalizi()).isEqualTo("redirect:/costs");
    }
}
