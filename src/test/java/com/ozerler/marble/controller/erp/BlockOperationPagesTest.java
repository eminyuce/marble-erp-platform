package com.ozerler.marble.controller.erp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BlockOperationPagesTest {

    @Test
    @DisplayName("blocks grid uses dedicated move and transfer pages instead of operation modals")
    void gridActionsNavigateToDedicatedPages() throws Exception {
        String grid = read("src/main/resources/static/js/blocks-grid.js");
        String index = read("src/main/resources/templates/erp/blocks/index.html");
        String detail = read("src/main/resources/templates/erp/blocks/detail.html");

        assertThat(grid)
                .contains("/blocks/\" + row.id + \"/move?targetType=PRODUCTION_YARD")
                .contains("/blocks/\" + row.id + \"/move?targetType=DISPATCH_YARD")
                .contains("/blocks/\" + row.id + \"/transfer-to-factory")
                .contains("/blocks/\" + row.id + \"/sell")
                .doesNotContain("openMoveBlock")
                .doesNotContain("openTransferBlock");

        assertThat(index)
                .doesNotContain("id=\"move-block-dialog\"")
                .doesNotContain("id=\"transfer-block-dialog\"")
                .contains("id=\"delete-block-dialog\"");

        assertThat(detail)
                .contains("/blocks/' + ${block.id} + '/move'")
                .contains("/blocks/' + ${block.id} + '/transfer-to-factory'")
                .doesNotContain("name=\"transportCost\"")
                .doesNotContain("Sahayı güncelle");
    }

    @Test
    @DisplayName("move and transfer pages include small help text and dedicated form layout")
    void operationPagesIncludeHelpAndFormChrome() throws Exception {
        String move = read("src/main/resources/templates/erp/blocks/move.html");
        String transfer = read("src/main/resources/templates/erp/blocks/transfer.html");

        assertThat(move)
                .contains("layout:decorate=\"~{layout/base}\"")
                .contains("erp-form-page")
                .contains("erp-ops-info-callout")
                .contains("fragments/erp-ops :: tip")
                .contains("id=\"block-move-form\"")
                .contains("name=\"targetType\"")
                .contains("name=\"description\"")
                .contains("id=\"submit-move-list-btn\"")
                .contains("id=\"submit-move-btn\"")
                .contains("Kaydet ve Listeye Dön")
                .contains("Kaydet ve Detaya Dön")
                .contains("Saha Taşıma Süreci");

        assertThat(transfer)
                .contains("layout:decorate=\"~{layout/base}\"")
                .contains("erp-form-page")
                .contains("erp-ops-info-callout")
                .contains("fragments/erp-ops :: tip")
                .contains("id=\"block-transfer-form\"")
                .contains("name=\"transportCost\"")
                .contains("id=\"submit-transfer-list-btn\"")
                .contains("id=\"submit-transfer-btn\"")
                .contains("Kaydet ve Listeye Dön")
                .contains("Kaydet ve Detaya Dön")
                .contains("Fabrika Sevkiyatı Süreci");
    }

    @Test
    @DisplayName("operation help files exist for the help button")
    void operationHelpFilesArePresent() throws Exception {
        String moveHelp = read("src/main/resources/help/block-move.html");
        String transferHelp = read("src/main/resources/help/block-transfer.html");

        assertThat(moveHelp).contains("data-title=\"Blok Saha Taşıma\"").contains("help-panel-list");
        assertThat(transferHelp).contains("data-title=\"Fabrikaya Sevk\"").contains("help-panel-list");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
