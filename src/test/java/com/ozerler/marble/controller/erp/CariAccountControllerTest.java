package com.ozerler.marble.controller.erp;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ozerler.marble.model.enums.BusinessUnit;

class CariAccountControllerTest {

    private final CariAccountController controller = new CariAccountController();

    @Test
    @DisplayName("accountsIndex redirects to definitions customers")
    void accountsIndexWithoutUnitRedirectsToCustomers() {
        String view = controller.accountsIndex(null);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers");
    }

    @Test
    @DisplayName("accountsIndex with unit redirects to definitions customers with unit query param")
    void accountsIndexWithUnitRedirectsWithQuery() {
        String view = controller.accountsIndex(BusinessUnit.QUARRY);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers?unit=QUARRY");
    }

    @Test
    @DisplayName("createAccount redirects to customer create form")
    void createAccountWithoutUnitRedirectsToCreate() {
        String view = controller.createAccount(null);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers/create");
    }

    @Test
    @DisplayName("createAccount with unit redirects to create with unit prefill")
    void createAccountWithUnitRedirectsWithUnit() {
        String view = controller.createAccount(BusinessUnit.FACTORY);
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/customers/create?unit=FACTORY");
    }

    @Test
    @DisplayName("suppliers redirects to suppliers definition list")
    void suppliersRedirectsToSuppliers() {
        String view = controller.suppliers();
        Assertions.assertThat(view).isEqualTo("redirect:/admin/definitions/suppliers");
    }

    @Test
    @DisplayName("projects redirects to projects list")
    void projectsRedirectsToProjects() {
        String view = controller.projects();
        Assertions.assertThat(view).isEqualTo("redirect:/projects");
    }
}
