package com.ozerler.marble.controller.erp;

import com.ozerler.marble.dto.GenealogyNodeDto;
import com.ozerler.marble.service.GenealogyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/genealogy")
@RequiredArgsConstructor
public class GenealogyController {

    private final GenealogyService genealogyService;
    private final org.springframework.context.MessageSource messageSource;

    @GetMapping
    public String genealogyIndex(@RequestParam(value = "code", required = false) String code,
                                 @RequestParam(value = "blockId", required = false) Long blockId,
                                 Model model,
                                 java.util.Locale locale) {

        model.addAttribute("allBlocks", genealogyService.getAllBlocks());

        GenealogyNodeDto tree = null;
        if (code != null && !code.isBlank()) {
            Optional<GenealogyNodeDto> result = genealogyService.traceBack(code);
            if (result.isPresent()) {
                tree = result.get();
                model.addAttribute("searchedCode", code);
            } else {
                model.addAttribute("errorMessage",
                        messageSource.getMessage("erp.genealogy.not_found", new Object[]{code}, locale));
            }
        } else if (blockId != null) {
            tree = genealogyService.buildTreeForBlock(blockId);
        } else {
            tree = genealogyService.getDefaultTree().orElse(null);
        }

        model.addAttribute("tree", tree);
        return "erp/genealogy/trace";
    }
}
