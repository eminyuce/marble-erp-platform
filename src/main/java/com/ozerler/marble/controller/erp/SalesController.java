package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.enums.SalesOrderStatus;
import com.ozerler.marble.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;
    private final MessageSource messageSource;

    @GetMapping
    public String salesIndex(Model model) {
        model.addAttribute("statuses", SalesOrderStatus.values());
        return "erp/sales/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<SalesOrderDto> getSalesData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return salesService.getSalesOrdersPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        populateSalesForm(model, locale);
        return "erp/sales/form";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam("orderNo") String orderNo,
                              @RequestParam("customerId") Long customerId,
                              @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                              @RequestParam(value = "notes", required = false) String notes,
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            SalesOrder order = salesService.createSalesOrder(orderNo, customerId, deliveryDate, notes);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.sales.create.success", new Object[]{order.getOrderNo()}, locale));
            return "redirect:/sales";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateSalesForm(model, locale);
            return "erp/sales/form";
        }
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable("id") Long id, Model model) {
        SalesOrder order = salesService.getOrderById(id);
        model.addAttribute("order", order);
        return "erp/sales/detail";
    }

    @PostMapping("/{id}/items")
    public String addItem(@PathVariable("id") Long id,
                          @RequestParam("description") String description,
                          @RequestParam("quantity") BigDecimal quantity,
                          @RequestParam(value = "unit", defaultValue = "m2") String unit,
                          @RequestParam("unitPrice") BigDecimal unitPrice,
                          @RequestParam(value = "slabId", required = false) Long slabId,
                          @RequestParam(value = "blockId", required = false) Long blockId) {
        salesService.addItemToOrder(id, description, quantity, unit, unitPrice, slabId, blockId);
        return "redirect:/sales/" + id;
    }

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable("id") Long id) {
        salesService.confirmOrder(id);
        return "redirect:/sales/" + id;
    }

    private void populateSalesForm(Model model, Locale locale) {
        model.addAttribute("customers", salesService.getAllCustomers());
        model.addAttribute("generatedOrderNo", salesService.generateOrderNo());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.sales.title.create", null, locale));
    }
}
