package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductionLazyAssociationTemplateTest {

    @Test
    @DisplayName("Factory list and polish pages never touch lazy FactoryWorkOrder.block")
    void factoryPagesDoNotDereferenceLazyBlock() throws Exception {
        String index = read("/templates/erp/production/index.html");
        String accept = read("/templates/erp/production/accept.html");
        String polish = read("/templates/erp/production/polish.html");

        assertDoesNotDereferenceLazyBlock(index);
        assertDoesNotDereferenceLazyBlock(accept);
        Assertions.assertThat(accept).contains("fragments/data-grid :: shell");
        assertDoesNotDereferenceLazyBlock(polish);
        Assertions.assertThat(polish).contains("fragments/data-grid :: shell");
        Assertions.assertThat(polish).contains("wo.blockCode");
    }

    private static void assertDoesNotDereferenceLazyBlock(String html) {
        Assertions.assertThat(html).doesNotContainPattern("wo\\.block(?![A-Za-z])");
    }

    private static String read(String resource) throws Exception {
        try (var in = ProductionLazyAssociationTemplateTest.class.getResourceAsStream(resource)) {
            Assertions.assertThat(in).as("classpath resource %s", resource).isNotNull();
            return new String(in.readAllBytes());
        }
    }
}
