package com.ozerler.marble.web;

import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.SupplierDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.SupplierType;

/**
 * Tedarikçi listesi. Sort keys match {@code GridPages.SUPPLIER_SORTS}. Type and unit filters already exist.
 */
public final class SupplierGrid {

    private SupplierGrid() {
    }

    public static DataGridView view(TabulatorResponse<SupplierDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (SupplierDto supplier : response.getData()) {
                if (supplier != null) {
                    rows.add(row(supplier));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "suppliers-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    public static GridRow row(SupplierDto supplier) {
        String id;
        if (supplier.getId() == null) {
            id = "";
        } else {
            id = supplier.getId().toString();
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = "/admin/definitions/suppliers/" + id + "/edit";
        }
        return GridRow.of(editUrl, editUrl, supplier.getId(), supplier.getCompanyName(), List.of(
                GridCell.chip(supplier.getId()),
                GridCell.text(supplier.getSupplierCode()),
                company(supplier.getCompanyName(), supplier.getAddress()),
                GridCell.badge(unitText(supplier.getBusinessUnitLabel())),
                GridCell.badge(supplier.getSupplierTypeLabel()),
                contactCell(supplier.getContactPerson(), supplier.getPhone(), supplier.getEmail()),
                GridCell.text(tax(supplier.getTaxOffice(), supplier.getTaxNumber()))
        ));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("supplierCode", "Tedarikçi Kodu", true, GridWidth.PX_150),
                GridColumn.text("companyName", "Firma / Ünvan", true, GridWidth.PX_220),
                GridColumn.filtered(
                        "businessUnitLabel",
                        "İşletme Birimi",
                        false,
                        GridWidth.PX_150,
                        "unit",
                        unitChoices()),
                GridColumn.filtered(
                        "supplierTypeLabel",
                        "Tedarik Tipi",
                        true,
                        GridWidth.PX_150,
                        "type",
                        typeChoices()),
                GridColumn.text("contactPerson", "Yetkili / İletişim", true, GridWidth.PX_200),
                GridColumn.text("", "Vergi Dairesi / No", false, GridWidth.PX_170));
    }

    private static GridCell company(String name, String address) {
        String title = GridTexts.text(name);
        String detail = GridTexts.text(address);
        if (title.isBlank()) {
            return GridCell.text(detail);
        }
        return GridCell.link(title, null, detail, null);
    }

    private static String unitText(String label) {
        String text = GridTexts.text(label);
        if (text.isBlank()) {
            return "Genel";
        }
        return text;
    }

    private static GridCell contactCell(String person, String phone, String email) {
        String name = GridTexts.text(person);
        String line = contactLine(phone, email);
        if (name.isBlank()) {
            return GridCell.text(line);
        }
        return GridCell.link(name, null, line, null);
    }

    private static String contactLine(String phone, String email) {
        List<String> parts = new ArrayList<>();
        String phoneText = GridTexts.text(phone);
        String emailText = GridTexts.text(email);
        if (!phoneText.isBlank()) {
            parts.add(phoneText);
        }
        if (!emailText.isBlank()) {
            parts.add(emailText);
        }
        return String.join(" ", parts);
    }

    private static String tax(String office, String number) {
        String officeText = GridTexts.text(office);
        String numberText = GridTexts.text(number);
        if (officeText.isBlank() && numberText.isBlank()) {
            return "";
        }
        if (officeText.isBlank()) {
            return numberText;
        }
        if (numberText.isBlank()) {
            return officeText;
        }
        return officeText + " / " + numberText;
    }

    private static List<GridColumn.FilterChoice> unitChoices() {
        return List.of(
                new GridColumn.FilterChoice("", "Tümü"),
                choice(BusinessUnit.QUARRY),
                choice(BusinessUnit.FACTORY),
                choice(BusinessUnit.WORKSHOP),
                choice(BusinessUnit.SITE),
                choice(BusinessUnit.GENERAL)
        );
    }

    private static GridColumn.FilterChoice choice(BusinessUnit unit) {
        return new GridColumn.FilterChoice(unit.name(), unit.getDisplayName());
    }

    private static List<GridColumn.FilterChoice> typeChoices() {
        return List.of(
                new GridColumn.FilterChoice("", "Tümü"),
                new GridColumn.FilterChoice(SupplierType.CONSUMABLE.name(), SupplierType.CONSUMABLE.getLabel()),
                new GridColumn.FilterChoice(SupplierType.CHEMICAL.name(), SupplierType.CHEMICAL.getLabel()),
                new GridColumn.FilterChoice(SupplierType.STONE.name(), SupplierType.STONE.getLabel()),
                new GridColumn.FilterChoice(SupplierType.EQUIPMENT.name(), SupplierType.EQUIPMENT.getLabel())
        );
    }
}
