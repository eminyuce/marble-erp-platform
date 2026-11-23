package com.ozerler.marble.controller.erp;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.service.MachineFuelService;

@ExtendWith(MockitoExtension.class)
class MachineFuelControllerTest {

    @Mock
    private MachineFuelService machineFuelService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private MachineFuelController controller;

    @Test
    void index_ReturnsFuelIndexViewWithReport() {
        Model model = new ExtendedModelMap();
        Mockito.when(machineFuelService.listViewsByPeriod(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(machineFuelService.getMonthlyReport(ArgumentMatchers.any())).thenReturn(List.of());
        Mockito.when(machineFuelService.quarryAndFactoryMachineViews()).thenReturn(List.of());
        Mockito.when(machineFuelService.periodSummary(ArgumentMatchers.any())).thenReturn(
                new com.ozerler.marble.dto.view.FuelPeriodSummary(BigDecimal.ZERO, BigDecimal.ZERO, 0, 0L));

        String view = controller.index("2026-09", model);

        Assertions.assertThat(view).isEqualTo("erp/machines/fuel-index");
        Assertions.assertThat(model.getAttribute("period")).isEqualTo("2026-09");
        Assertions.assertThat(model.getAttribute("fuelEntries")).isNotNull();
        Assertions.assertThat(model.getAttribute("monthlyReport")).isNotNull();
    }

    @Test
    void showCreateForm_ReturnsFuelFormView() {
        Model model = new ExtendedModelMap();
        Mockito.when(machineFuelService.quarryAndFactoryMachineViews()).thenReturn(List.of());

        String view = controller.showCreateForm(model);

        Assertions.assertThat(view).isEqualTo("erp/machines/fuel-form");
        Assertions.assertThat(model.getAttribute("machines")).isNotNull();
        Assertions.assertThat(model.getAttribute("defaultDate")).isNotNull();
    }

    @Test
    void createFuelEntry_WhenValid_RedirectsToIndex() {
        com.ozerler.marble.dto.view.MachineRef machine = new com.ozerler.marble.dto.view.MachineRef();
        machine.setId(1L);
        machine.setName("CAT 320");
        machine.setBusinessUnit(BusinessUnit.QUARRY);
        com.ozerler.marble.dto.view.MachineFuelEntryView entry = new com.ozerler.marble.dto.view.MachineFuelEntryView();
        entry.setId(10L);
        entry.setMachine(machine);
        entry.setLitres(new BigDecimal("450"));

        Mockito.when(machineFuelService.recordFuelView(
                ArgumentMatchers.eq(1L), ArgumentMatchers.any(), ArgumentMatchers.eq(new BigDecimal("450")), ArgumentMatchers.eq(new BigDecimal("42.50")),
                ArgumentMatchers.eq(new BigDecimal("1200.5")), ArgumentMatchers.eq("F-01"), ArgumentMatchers.eq("Ali"), ArgumentMatchers.eq("Veli"), ArgumentMatchers.eq("Test note")))
                .thenReturn(entry);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.createFuelEntry(
                1L, LocalDate.now(), new BigDecimal("450"), new BigDecimal("42.50"),
                new BigDecimal("1200.5"), "F-01", "Ali", "Veli", "Test note",
                Locale.of("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/machines/fuel");
        Assertions.assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void deleteFuelEntry_DeletesAndRedirects() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.deleteFuelEntry(5L, Locale.of("tr"), redirectAttributes);

        Mockito.verify(machineFuelService).deleteFuel(5L);
        Assertions.assertThat(view).isEqualTo("redirect:/machines/fuel");
        Assertions.assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void getFuelData_ReturnsTabulatorResponse() {
        Mockito.when(machineFuelService.getFuelPaged(1, 25, "CAT", "2026-09", "QUARRY", 1L, "entryDate", "desc"))
                .thenReturn(com.ozerler.marble.dto.TabulatorResponse.of(List.of(), 1, 0));

        var response = controller.getFuelData(1, 25, "CAT", "2026-09", "QUARRY", 1L, "entryDate", "desc");

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getData()).isEmpty();
    }
}
