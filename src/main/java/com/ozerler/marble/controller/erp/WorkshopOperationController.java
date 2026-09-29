package com.ozerler.marble.controller.erp;

import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.model.OperationWorkOrder;
import com.ozerler.marble.model.enums.BusinessUnit;
import com.ozerler.marble.model.enums.OperationWorkOrderStatus;
import com.ozerler.marble.service.OperationWorkOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/operations/workshop")
@RequiredArgsConstructor
@Slf4j
public class WorkshopOperationController extends AbstractController {

    private final OperationWorkOrderService workOrderService;

    @GetMapping
    public String index(@RequestParam(value = "status", required = false) OperationWorkOrderStatus status,
                        @RequestParam(value = "page", defaultValue = "0") int page,
                        @RequestParam(value = "size", defaultValue = "20") int size,
                        Model model) {

        Page<OperationWorkOrder> ordersPage = workOrderService.searchOrders(BusinessUnit.WORKSHOP, status, null, null, null, null, page, size);
        model.addAttribute("orders", ordersPage.getContent());
        model.addAttribute("page", ordersPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeSection", "workshop");
        return "operations/workshop/index";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        return "redirect:/operations/workshop";
    }
}
