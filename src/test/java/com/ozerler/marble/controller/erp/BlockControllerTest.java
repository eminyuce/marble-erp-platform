package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.QuarryBlockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    @Mock
    private QuarryBlockService quarryBlockService;

    @Mock
    private org.springframework.context.MessageSource messageSource;

    @Mock
    private com.ozerler.marble.service.FileStorageService fileStorageService;

    @InjectMocks
    private BlockController blockController;

    @Test
    @DisplayName("deleteBlockApi should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnDeleteBlockApi() {
        Long blockId = 100L;
        com.ozerler.marble.model.Block block = com.ozerler.marble.model.Block.builder().id(blockId).blockCode("BLK-100").build();
        org.mockito.Mockito.when(quarryBlockService.getBlockById(blockId)).thenReturn(block);
        org.mockito.Mockito.when(messageSource.getMessage(org.mockito.ArgumentMatchers.eq("erp.block.delete.success"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("BLK-100 kodlu blok silindi.");

        BackEndResponse response = blockController.deleteBlockApi(blockId, java.util.Locale.forLanguageTag("tr"));

        verify(quarryBlockService).deleteBlock(blockId);
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("BLK-100 kodlu blok silindi.");
    }

    @Test
    @DisplayName("deleteBlockApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnDeleteBlockApiError() {
        Long blockId = 100L;
        com.ozerler.marble.model.Block block = com.ozerler.marble.model.Block.builder().id(blockId).blockCode("BLK-100").build();
        org.mockito.Mockito.when(quarryBlockService.getBlockById(blockId)).thenReturn(block);
        doThrow(new IllegalStateException("Blok silinemez")).when(quarryBlockService).deleteBlock(blockId);

        BackEndResponse response = blockController.deleteBlockApi(blockId, java.util.Locale.forLanguageTag("tr"));

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        assertThat(response.getServiceStatus().getStatus().getMessage()).contains("deleteBlock");
    }

    @Test
    @DisplayName("transferToFactory should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnTransfer() {
        Long blockId = 100L;
        BigDecimal transportCost = new BigDecimal("1500.00");

        BackEndResponse response = blockController.transferToFactoryApi(blockId, transportCost);

        verify(quarryBlockService).transferToFactory(blockId, transportCost);
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Transfer to factory successful");
    }

    @Test
    @DisplayName("transferToFactory should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnTransferError() {
        Long blockId = 100L;
        BigDecimal transportCost = new BigDecimal("1500.00");
        doThrow(new RuntimeException("Database error")).when(quarryBlockService).transferToFactory(blockId, transportCost);

        BackEndResponse response = blockController.transferToFactoryApi(blockId, transportCost);

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        assertThat(response.getServiceStatus().getStatus().getMessage()).contains("transferToFactory");
    }

    @Test
    @DisplayName("sellBlock should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnSell() {
        Long blockId = 100L;

        BackEndResponse response = blockController.sellBlock(blockId, 9L, new BigDecimal("500000"));

        verify(quarryBlockService).sellBlockExternally(
                org.mockito.ArgumentMatchers.eq(blockId),
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("500000")),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.isNull());
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Sell block successful");
    }

    @Test
    @DisplayName("sellBlock should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnSellError() {
        Long blockId = 100L;
        doThrow(new RuntimeException("Lock conflict")).when(quarryBlockService)
                .sellBlockExternally(
                        org.mockito.ArgumentMatchers.eq(blockId),
                        org.mockito.ArgumentMatchers.eq(9L),
                        org.mockito.ArgumentMatchers.eq(new BigDecimal("500000")),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull());

        BackEndResponse response = blockController.sellBlock(blockId, 9L, new BigDecimal("500000"));

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        assertThat(response.getServiceStatus().getStatus().getMessage()).contains("sellBlock");
    }

    @Test
    @DisplayName("moveToYardApi should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnMove() {
        Long blockId = 100L;

        BackEndResponse response = blockController.moveToYardApi(
                blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        verify(quarryBlockService).moveToYard(blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Move to yard successful");
    }

    @Test
    @DisplayName("moveToYardApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnMoveError() {
        Long blockId = 100L;
        doThrow(new RuntimeException("Not at quarry")).when(quarryBlockService)
                .moveToYard(blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        BackEndResponse response = blockController.moveToYardApi(
                blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        assertThat(response.getServiceStatus().getStatus().getMessage()).contains("moveToYard");
    }

    @Test
    @DisplayName("deleteBlock form action redirects to /blocks on success")
    void shouldRedirectToBlocksOnDeleteBlockFormSuccess() {
        Long blockId = 100L;
        com.ozerler.marble.model.Block block = com.ozerler.marble.model.Block.builder().id(blockId).blockCode("BLK-100").build();
        org.mockito.Mockito.when(quarryBlockService.getBlockById(blockId)).thenReturn(block);
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        verify(quarryBlockService).deleteBlock(blockId);
        assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("deleteBlock form action redirects to /blocks/{id} on error")
    void shouldRedirectToBlockDetailOnDeleteBlockFormError() {
        Long blockId = 100L;
        com.ozerler.marble.model.Block block = com.ozerler.marble.model.Block.builder().id(blockId).blockCode("BLK-100").build();
        org.mockito.Mockito.when(quarryBlockService.getBlockById(blockId)).thenReturn(block);
        doThrow(new IllegalStateException("Kullanımda olan blok silinemez")).when(quarryBlockService).deleteBlock(blockId);
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("createBlock passes locationType to quarryBlockService.registerBlock")
    void shouldPassLocationTypeToRegisterBlock() {
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();

        String view = blockController.createBlock(
                1L, "BLK-NEW-01", StockLocationType.DISPATCH_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, null, "A-BLOK",
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD, null, "A-BLOK");
        assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("createBlock passes fileIds to quarryBlockService.registerBlock")
    void shouldPassFileIdsToRegisterBlock() {
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        java.util.List<Long> fileIds = java.util.List.of(55L, 56L);

        String view = blockController.createBlock(
                1L, "BLK-NEW-02", StockLocationType.PRODUCTION_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, fileIds, "A3",
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-02", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds, "A3");
        assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("updateBlock passes locationType and fileIds to quarryBlockService.updateBlock")
    void shouldPassLocationTypeToUpdateBlock() {
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        java.util.List<Long> fileIds = java.util.List.of(77L);

        String view = blockController.updateBlock(
                10L, 1L, "BLK-UPD-01", StockLocationType.DISPATCH_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                new BigDecimal("5000"), "Not", null, fileIds, "A-BLOK",
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).updateBlock(
                10L, 1L, "BLK-UPD-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                new BigDecimal("5000"), "Not", null, StockLocationType.DISPATCH_YARD, fileIds, "A-BLOK");
        assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("showMoveForm opens dedicated page with requested target yard")
    void shouldOpenMoveFormForQuarryBlock() {
        org.mockito.Mockito.when(quarryBlockService.getBlockWithDetails(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.DISPATCH_YARD, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        assertThat(view).isEqualTo("erp/blocks/move");
        assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
        assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showMoveForm defaults target to opposite quarry yard")
    void shouldDefaultMoveTargetToOppositeYard() {
        org.mockito.Mockito.when(quarryBlockService.getBlockWithDetails(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, null, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        assertThat(view).isEqualTo("erp/blocks/move");
        assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
    }

    @Test
    @DisplayName("showMoveForm redirects sold blocks to detail")
    void shouldRedirectSoldBlockAwayFromMoveForm() {
        org.mockito.Mockito.when(quarryBlockService.getBlockWithDetails(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.SOLD, StockLocationType.DISPATCH_YARD));
        org.mockito.Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.already_sold"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("already sold");
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.PRODUCTION_YARD, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("moveToYard form redirects to move page on error")
    void shouldRedirectToMovePageOnMoveFormError() {
        doThrow(new IllegalArgumentException("Blok ocak sahasında değil")).when(quarryBlockService)
                .moveToYard(100L, StockLocationType.DISPATCH_YARD, "Stok sahası");
        org.mockito.Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("common.error.prefix"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Error: Blok ocak sahasında değil");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/blocks/100/move?targetType=DISPATCH_YARD");
    }

    @Test
    @DisplayName("showTransferForm opens dedicated page for dispatch-yard blocks")
    void shouldOpenTransferFormForDispatchYardBlock() {
        org.mockito.Mockito.when(quarryBlockService.getBlockWithDetails(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.DISPATCH_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        assertThat(view).isEqualTo("erp/blocks/transfer");
        assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showTransferForm redirects production-yard blocks to detail")
    void shouldRedirectProductionYardBlockAwayFromTransferForm() {
        org.mockito.Mockito.when(quarryBlockService.getBlockWithDetails(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        org.mockito.Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("error.block.dispatch.not_in_dispatch_yard"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("only dispatch");
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("transferToFactory form redirects to transfer page on error")
    void shouldRedirectToTransferPageOnTransferFormError() {
        doThrow(new IllegalArgumentException("only dispatch")).when(quarryBlockService)
                .transferToFactory(100L, new BigDecimal("12500"));
        org.mockito.Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("common.error.prefix"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Error: only dispatch");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        assertThat(view).isEqualTo("redirect:/blocks/100/transfer-to-factory");
    }

    private static com.ozerler.marble.model.Block quarryBlock(
            com.ozerler.marble.model.enums.BlockStatus status,
            StockLocationType locationType) {
        com.ozerler.marble.model.StockLocation location = locationType == null ? null
                : com.ozerler.marble.model.StockLocation.builder()
                .id(1L)
                .code(locationType.name())
                .name(locationType.name())
                .businessUnit(locationType.getBusinessUnit())
                .locationType(locationType)
                .build();
        return com.ozerler.marble.model.Block.builder()
                .id(100L)
                .blockCode("BLK-100")
                .status(status)
                .currentLocation(location)
                .build();
    }
}
