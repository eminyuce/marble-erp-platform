package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.SalesOrderDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.SalesOrder;
import com.ozerler.marble.model.enums.SalesOrderStatus;
import com.ozerler.marble.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

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
    public String showCreateModal(Model model) {
        model.addAttribute("customers", salesService.getAllCustomers());
        model.addAttribute("generatedOrderNo", "SAT-" + LocalDate.now().getYear() + "-" + String.format("%05d", (int) (Math.random() * 99999)));
        return "erp/sales/form :: salesModalContent";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam("orderNo") String orderNo,
                              @RequestParam("customerId") Long customerId,
                              @RequestParam(value = "deliveryDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
                              @RequestParam(value = "notes", required = false) String notes,
                              Model model) {
        try {
            SalesOrder order = salesService.createSalesOrder(orderNo, customerId, deliveryDate, notes);
            model.addAttribute("success", true);
            model.addAttribute("message", "Satış siparişi " + order.getOrderNo() + " başarıyla oluşturuldu.");
            return "erp/sales/form :: salesModalSuccess";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Hata: " + e.getMessage());
            model.addAttribute("customers", salesService.getAllCustomers());
            return "erp/sales/form :: salesModalContent";
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
}
