package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionLazyAssociationTemplateTest {

    @Test
    @DisplayName("Factory list and polish pages never touch lazy FactoryWorkOrder.block")
    void factoryPagesDoNotDereferenceLazyBlock() throws Exception {
        String index = read("/templates/erp/production/index.html");
        String polish = read("/templates/erp/production/polish.html");

        assertDoesNotDereferenceLazyBlock(index);
        assertThat(index).contains("wo.blockCode");
        assertThat(index).contains("wo.statusLabel");
        assertDoesNotDereferenceLazyBlock(polish);
        assertThat(polish).contains("wo.blockCode");
    }

    private static void assertDoesNotDereferenceLazyBlock(String html) {
        assertThat(html).doesNotContainPattern("wo\\.block(?![A-Za-z])");
    }

    private static String read(String resource) throws Exception {
        try (var in = ProductionLazyAssociationTemplateTest.class.getResourceAsStream(resource)) {
            assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
