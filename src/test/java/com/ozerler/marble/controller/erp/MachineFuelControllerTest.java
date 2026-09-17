package com.ozerler.marble.controller.erp;

import com.ozerler.marble.model.Machine;
import com.ozerler.marble.model.MachineFuelEntry;
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
        when(machineFuelService.listByPeriod(any())).thenReturn(List.of());
        when(machineFuelService.getMonthlyReport(any())).thenReturn(List.of());
        when(machineFuelService.quarryAndFactoryMachines()).thenReturn(List.of());

        String view = controller.index("2026-09", model);

        assertThat(view).isEqualTo("erp/machines/fuel-index");
        assertThat(model.getAttribute("period")).isEqualTo("2026-09");
        assertThat(model.getAttribute("fuelEntries")).isNotNull();
        assertThat(model.getAttribute("monthlyReport")).isNotNull();
    }

    @Test
    void showCreateForm_ReturnsFuelFormView() {
        Model model = new ExtendedModelMap();
        when(machineFuelService.quarryAndFactoryMachines()).thenReturn(List.of());

        String view = controller.showCreateForm(model);

        assertThat(view).isEqualTo("erp/machines/fuel-form");
        assertThat(model.getAttribute("machines")).isNotNull();
        assertThat(model.getAttribute("defaultDate")).isNotNull();
    }

    @Test
    void createFuelEntry_WhenValid_RedirectsToIndex() {
        Machine machine = Machine.builder().id(1L).name("CAT 320").businessUnit(BusinessUnit.QUARRY).build();
        MachineFuelEntry entry = MachineFuelEntry.builder().id(10L).machine(machine).litres(new BigDecimal("450")).build();

        when(machineFuelService.recordFuel(
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
}
