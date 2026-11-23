package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.MachineFuelDto;
import com.ozerler.marble.dto.TabulatorResponse;

/**
 * Mazot dolum listesi. Sort fields match {@code MachineFuelService.getFuelPaged}.
 */
public final class FuelGrid {

    private FuelGrid() {
    }

    public static DataGridView view(TabulatorResponse<MachineFuelDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (MachineFuelDto entry : response.getData()) {
                rows.add(row(entry));
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "fuel-table", 4, columns(), rows);
    }

    public static GridRow row(MachineFuelDto entry) {
        String litres = GridTexts.quantity(entry.getLitres());
        String name = GridWindow.clean(entry.getMachineName());
        String deleteName = litres.isBlank() ? name : name + " — " + litres + " L";
        return GridRow.of("", "", entry.getId(), deleteName, List.of(
                GridCell.chip(entry.getId()),
                GridCell.text(GridWindow.clean(entry.getReceiptNo())),
                GridCell.link(name, null, GridWindow.clean(entry.getMachineCode()), null),
                GridCell.text(GridTexts.date(entry.getEntryDate())),
                GridCell.text(GridWindow.clean(entry.getBusinessUnitLabel())),
                GridCell.text(litres.isBlank() ? "" : litres + " L"),
                GridCell.text(GridTexts.money(entry.getPricePerLitre())),
                GridCell.text(GridTexts.money(entry.getTotalAmount())),
                GridCell.text(hours(entry)),
                GridCell.text(handover(entry))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("", "Id", false, 110),
                GridColumn.text("", "Fiş No", false, 120),
                GridColumn.text("machineName", "Makine", true, 180),
                GridColumn.text("entryDate", "Tarih", true, 120),
                GridColumn.text("", "Birim", false, 130),
                GridColumn.text("litres", "Litre (L)", true, 120),
                GridColumn.text("", "Litre Fiyatı", false, 130),
                GridColumn.text("totalAmount", "Toplam Tutar", true, 140),
                GridColumn.text("workingHoursOrKm", "Çalışma Saati / Km", true, 160),
                GridColumn.text("", "Teslim Alan / Veren", false, 180)
        );
    }

    private static String hours(MachineFuelDto entry) {
        String value = GridTexts.quantity(entry.getWorkingHoursOrKm());
        return value.isBlank() ? "" : value + " sa/km";
    }

    private static String handover(MachineFuelDto entry) {
        String received = GridWindow.clean(entry.getReceivedBy());
        String issued = GridWindow.clean(entry.getIssuedBy());
        if (!received.isBlank() && !issued.isBlank()) {
            return received + " / " + issued;
        }
        return received.isBlank() ? issued : received;
    }
}
