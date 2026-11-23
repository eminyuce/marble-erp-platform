package com.ozerler.marble.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.ozerler.marble.dto.CustomerDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.CustomerType;

/**
 * Cari listesi. Sort keys match {@code GridPages.CUSTOMER_SORTS}. Type and unit filters already exist.
 */
public final class CustomerGrid {

    private static final BigDecimal BALANCE_STEP = new BigDecimal("0.009");

    private CustomerGrid() {
    }

    public static DataGridView view(TabulatorResponse<CustomerDto> response, int page, int size,
                                    String sort, String direction) {
        List<GridRow> rows = new ArrayList<>();
        if (response != null && response.getData() != null) {
            for (CustomerDto customer : response.getData()) {
                if (customer != null) {
                    rows.add(row(customer));
                }
            }
        }
        return GridAssembler.from(response, page, size, sort, direction, "customers-table",
                GridAssembler.NO_FROZEN_COLUMNS, columns(), rows);
    }

    public static GridRow row(CustomerDto customer) {
        String id;
        if (customer.getId() == null) {
            id = "";
        } else {
            id = customer.getId().toString();
        }
        String transactions;
        if (id.isEmpty()) {
            transactions = "";
        } else {
            transactions = "/admin/definitions/customers/" + id + "/transactions";
        }
        String editUrl;
        if (id.isEmpty()) {
            editUrl = "";
        } else {
            editUrl = "/admin/definitions/customers/" + id + "/edit";
        }
        String unit = unitText(customer.getBusinessUnitLabel());
        String deleteKey;
        if (customer.getId() == null) {
            deleteKey = "";
        } else {
            deleteKey = customer.getId().toString();
        }
        return new GridRow(
                transactions,
                editUrl,
                deleteKey,
                customer.getCompanyName(),
                customerActions(transactions, editUrl, customer),
                List.of(
                        GridCell.chip(customer.getId()),
                        GridCell.link(customer.getCustomerCode(), transactions),
                        GridCell.link(
                                customer.getCompanyName(),
                                transactions,
                                companyDetail(customer),
                                companyDetailHref(customer)),
                        GridCell.badge(unit),
                        GridCell.badge(customer.getCustomerTypeLabel()),
                        contact(
                                customer.getContactPerson(),
                                customer.getPhone(),
                                customer.getEmail()),
                        GridCell.text(balance(customer.getBalance()))));
    }

    private static List<GridColumn> columns() {
        return List.of(
                GridColumn.text("id", "Id", true, GridWidth.PX_110),
                GridColumn.text("customerCode", "Cari Kodu", true, GridWidth.PX_140),
                GridColumn.text("companyName", "Firma / Ünvan", true, GridWidth.PX_240),
                GridColumn.filtered("businessUnitLabel", "İşletme Birimi", false, GridWidth.PX_150, "unit", units()),
                GridColumn.filtered("customerTypeLabel", "Müşteri Tipi", true, GridWidth.PX_150, "type", types()),
                GridColumn.text("contactPerson", "Yetkili / İletişim", true, GridWidth.PX_200),
                GridColumn.text("balance", "Bakiye", false, GridWidth.PX_150)
        );
    }

    private static String companyDetail(CustomerDto customer) {
        String address = GridTexts.text(customer.getAddress());
        if (customer.getProjectCount() > 0) {
            String projects = customer.getProjectCount() + " Proje";
            if (address.isBlank()) {
                return projects;
            }
            return address + " · " + projects;
        }
        if ("SITE".equals(customer.getBusinessUnit())) {
            if (address.isBlank()) {
                return "+ Proje Aç";
            }
            return address + " · + Proje Aç";
        }
        return address;
    }

    private static String companyDetailHref(CustomerDto customer) {
        if (customer.getId() == null) {
            return null;
        }
        if (customer.getProjectCount() > 0) {
            return "/projects?customerId=" + customer.getId();
        }
        if ("SITE".equals(customer.getBusinessUnit())) {
            return "/projects/create?customerId=" + customer.getId();
        }
        return null;
    }

    private static String unitText(String label) {
        String text = GridTexts.text(label);
        if (text.isBlank()) {
            return "Genel";
        }
        return text;
    }

    private static String balance(BigDecimal value) {
        BigDecimal amount;
        if (value == null) {
            amount = BigDecimal.ZERO;
        } else {
            amount = value;
        }
        if (amount.compareTo(BALANCE_STEP) > 0) {
            return GridTexts.money(amount) + " (B)";
        }
        if (amount.compareTo(BALANCE_STEP.negate()) < 0) {
            return GridTexts.money(amount.abs()) + " (A)";
        }
        return "0,00 ₺";
    }

    private static GridCell contact(String person, String phone, String email) {
        String name = GridTexts.text(person);
        String line = join(phone, email);
        if (name.isBlank()) {
            return GridCell.text(line);
        }
        return GridCell.link(name, null, line, null);
    }

    private static String join(String phone, String email) {
        String left = GridTexts.text(phone);
        String right = GridTexts.text(email);
        if (left.isBlank()) {
            return right;
        }
        if (right.isBlank()) {
            return left;
        }
        return left + " " + right;
    }

    private static List<GridColumn.FilterChoice> units() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (BusinessUnit unit : BusinessUnit.values()) {
            choices.add(new GridColumn.FilterChoice(unit.name(), unit.getDisplayName()));
        }
        return choices;
    }

    private static List<GridColumn.FilterChoice> types() {
        List<GridColumn.FilterChoice> choices = new ArrayList<>();
        choices.add(new GridColumn.FilterChoice("", "Tümü"));
        for (CustomerType type : CustomerType.values()) {
            choices.add(new GridColumn.FilterChoice(type.name(), type.getLabel()));
        }
        return choices;
    }

    private static List<GridAction> customerActions(String transactions, String editUrl,
                                                    CustomerDto customer) {
        List<GridAction> items = new ArrayList<>();
        if (!transactions.isBlank()) {
            items.add(GridAction.link("Hesap Hareketleri", "file-text", transactions));
        }
        if (!editUrl.isBlank()) {
            items.add(GridAction.link("Düzenle", "edit-3", editUrl));
        }
        if (customer.getId() != null) {
            items.add(GridAction.delete("Sil", customer.getId(), customer.getCompanyName()));
        }
        return items;
    }
}
