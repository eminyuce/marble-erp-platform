package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.controller.AbstractController;
import com.ozerler.marble.dto.BlockDto;
import com.ozerler.marble.dto.TabulatorResponse;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.model.response.ServiceStatus;
import com.ozerler.marble.model.response.Status;
import com.ozerler.marble.service.QuarryBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController extends AbstractController {

    private final QuarryBlockService quarryBlockService;
    private final MessageSource messageSource;

    @GetMapping
    public String blocksIndex(Model model) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        return "erp/blocks/index";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public TabulatorResponse<BlockDto> getBlocksData(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDir", required = false) String sortDir) {

        return quarryBlockService.getBlocksPaged(page, size, search, sortField, sortDir);
    }

    @GetMapping("/create")
    public String showCreateForm(Locale locale, Model model) {
        populateBlockForm(model, locale);
        return "erp/blocks/form";
    }

    @PostMapping("/create")
    public String createBlock(@RequestParam("quarryId") Long quarryId,
                              @RequestParam("blockCode") String blockCode,
                              @RequestParam(value = "extractionDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate extractionDate,
                              @RequestParam("widthCm") int widthCm,
                              @RequestParam("lengthCm") int lengthCm,
                              @RequestParam("heightCm") int heightCm,
                              @RequestParam("actualWeightKg") BigDecimal actualWeightKg,
                              @RequestParam("stoneType") String stoneType,
                              @RequestParam(value = "colorTone", required = false) String colorTone,
                              @RequestParam("qualityGrade") QualityGrade qualityGrade,
                              @RequestParam(value = "crackLevel", defaultValue = "0") int crackLevel,
                              @RequestParam("extractionCost") BigDecimal extractionCost,
                              @RequestParam(value = "notes", required = false) String notes,
                              @RequestParam(value = "photoUrls", required = false) String photoUrls,
                              Locale locale,
                              Model model,
                              RedirectAttributes redirectAttributes) {

        try {
            quarryBlockService.registerBlock(quarryId, blockCode, extractionDate, widthCm, lengthCm, heightCm,
                    actualWeightKg, stoneType, colorTone, qualityGrade, crackLevel, extractionCost, notes, photoUrls);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageSource.getMessage("erp.block.create.success", null, locale));
            return "redirect:/blocks";
        } catch (Exception e) {
            model.addAttribute("errorMessage",
                    messageSource.getMessage("common.error.prefix", new Object[]{e.getMessage()}, locale));
            populateBlockForm(model, locale);
            return "erp/blocks/form";
        }
    }

    @PostMapping("/{id}/transfer-to-factory")
    public @ResponseBody BackEndResponse transferToFactory(@PathVariable("id") Long id,
                                                           @RequestParam("transportCost") BigDecimal transportCost) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Transferring block {} to factory with cost {}", id, transportCost);
            quarryBlockService.transferToFactory(id, transportCost);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Transfer to factory successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in transferToFactory block {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "transferToFactory", Constants.ERR_FATAL);
        }

        return ber;
    }

    @PostMapping("/{id}/sell")
    public @ResponseBody BackEndResponse sellBlock(@PathVariable("id") Long id) {
        BackEndResponse ber = new BackEndResponse();
        ServiceStatus serviceStatus = new ServiceStatus();
        Status status = new Status();
        status.setErrorCode(Constants.NO_ERR);

        try {
            log.info("Selling block externally with id {}", id);
            quarryBlockService.sellBlockExternally(id);

            ResponseEntity<Void> resp = ResponseEntity.ok().build();
            ber.setResponse(resp);
            serviceStatus.setHttpStatus(HttpStatus.OK);
            status.setMessage("Sell block successful");
            serviceStatus.setStatus(status);
            ber.setServiceStatus(serviceStatus);
        } catch (Exception e) {
            log.error("A serious error occurred in sellBlock {}", id, e);
            ber = buildFatalResponse(ber, serviceStatus, status, "sellBlock", Constants.ERR_FATAL);
        }

        return ber;
    }

    private void populateBlockForm(Model model, Locale locale) {
        model.addAttribute("quarries", quarryBlockService.getAllQuarries());
        model.addAttribute("qualityGrades", QualityGrade.values());
        model.addAttribute("pageTitle", messageSource.getMessage("erp.block.title.create", null, locale));
    }
}
