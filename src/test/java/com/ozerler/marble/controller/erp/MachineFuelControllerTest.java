package com.ozerler.marble.controller.erp;

import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.service.MachineFuelService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        when(machineFuelService.listViewsByPeriod(any())).thenReturn(List.of());
        when(machineFuelService.getMonthlyReport(any())).thenReturn(List.of());
        when(machineFuelService.quarryAndFactoryMachineViews()).thenReturn(List.of());
        when(machineFuelService.periodSummary(any())).thenReturn(
                new com.ozerler.marble.dto.view.FuelPeriodSummary(BigDecimal.ZERO, BigDecimal.ZERO, 0, 0L));

        String view = controller.index("2026-09", model);

        assertThat(view).isEqualTo("erp/machines/fuel-index");
        assertThat(model.getAttribute("period")).isEqualTo("2026-09");
        assertThat(model.getAttribute("fuelEntries")).isNotNull();
        assertThat(model.getAttribute("monthlyReport")).isNotNull();
    }

    @Test
    void showCreateForm_ReturnsFuelFormView() {
        Model model = new ExtendedModelMap();
        when(machineFuelService.quarryAndFactoryMachineViews()).thenReturn(List.of());

        String view = controller.showCreateForm(model);

        assertThat(view).isEqualTo("erp/machines/fuel-form");
        assertThat(model.getAttribute("machines")).isNotNull();
        assertThat(model.getAttribute("defaultDate")).isNotNull();
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

        when(machineFuelService.recordFuelView(
                eq(1L), any(), eq(new BigDecimal("450")), eq(new BigDecimal("42.50")),
                eq(new BigDecimal("1200.5")), eq("F-01"), eq("Ali"), eq("Veli"), eq("Test note")))
                .thenReturn(entry);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.createFuelEntry(
                1L, LocalDate.now(), new BigDecimal("450"), new BigDecimal("42.50"),
                new BigDecimal("1200.5"), "F-01", "Ali", "Veli", "Test note",
                Locale.of("tr"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/machines/fuel");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void deleteFuelEntry_DeletesAndRedirects() {
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.deleteFuelEntry(5L, Locale.of("tr"), redirectAttributes);

        verify(machineFuelService).deleteFuel(5L);
        assertThat(view).isEqualTo("redirect:/machines/fuel");
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void getFuelData_ReturnsTabulatorResponse() {
        when(machineFuelService.getFuelPaged(1, 25, "CAT", "2026-09", "QUARRY", 1L, "entryDate", "desc"))
                .thenReturn(com.ozerler.marble.dto.TabulatorResponse.of(List.of(), 1, 0));

        var response = controller.getFuelData(1, 25, "CAT", "2026-09", "QUARRY", 1L, "entryDate", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getData()).isEmpty();
    }
}
