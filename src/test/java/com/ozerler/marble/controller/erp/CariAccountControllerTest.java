package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.BusinessUnit;

class CariAccountControllerTest {

    private final CariAccountController controller = new CariAccountController();

    @Test
    @DisplayName("accountsIndex redirects to definitions customers")
    void accountsIndex_WithoutUnit_RedirectsToCustomers() {
        String view = controller.accountsIndex(null);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers");
    }

    @Test
    @DisplayName("accountsIndex with unit redirects to definitions customers with unit query param")
    void accountsIndex_WithUnit_RedirectsWithQuery() {
        String view = controller.accountsIndex(BusinessUnit.QUARRY);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers?unit=QUARRY");
    }

    @Test
    @DisplayName("createAccount redirects to customer create form")
    void createAccount_WithoutUnit_RedirectsToCreate() {
        String view = controller.createAccount(null);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers/create");
    }

    @Test
    @DisplayName("createAccount with unit redirects to create with unit prefill")
    void createAccount_WithUnit_RedirectsWithUnit() {
        String view = controller.createAccount(BusinessUnit.FACTORY);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers/create?unit=FACTORY");
    }

    @Test
    @DisplayName("suppliers redirects to suppliers definition list")
    void suppliers_RedirectsToSuppliers() {
        String view = controller.suppliers();
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/suppliers");
    }

    @Test
    @DisplayName("projects redirects to projects list")
    void projects_RedirectsToProjects() {
        String view = controller.projects();
        Assertions.assertThat(view).isEqualTo("redirect:/projects");
    }
}
