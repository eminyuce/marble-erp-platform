package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.QuarryBlockService;

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
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.when(messageSource.getMessage(org.mockito.ArgumentMatchers.eq("erp.block.delete.success"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn("BLK-100 kodlu blok silindi.");

        BackEndResponse response = blockController.deleteBlockApi(blockId, java.util.Locale.forLanguageTag("tr"));

        Mockito.verify(quarryBlockService).deleteBlock(blockId);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("BLK-100 kodlu blok silindi.");
    }

    @Test
    @DisplayName("deleteBlockApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnDeleteBlockApiError() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.doThrow(new IllegalStateException("Blok silinemez")).when(quarryBlockService).deleteBlock(blockId);

        BackEndResponse response = blockController.deleteBlockApi(blockId, java.util.Locale.forLanguageTag("tr"));

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).contains("deleteBlock");
    }

    @Test
    @DisplayName("transferToFactory should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnTransfer() {
        Long blockId = 100L;
        BigDecimal transportCost = new BigDecimal("1500.00");

        BackEndResponse response = blockController.transferToFactoryApi(blockId, transportCost);

        Mockito.verify(quarryBlockService).transferToFactory(blockId, transportCost);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Transfer to factory successful");
    }

    @Test
    @DisplayName("transferToFactory should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnTransferError() {
        Long blockId = 100L;
        BigDecimal transportCost = new BigDecimal("1500.00");
        Mockito.doThrow(new RuntimeException("Database error")).when(quarryBlockService).transferToFactory(blockId, transportCost);

        BackEndResponse response = blockController.transferToFactoryApi(blockId, transportCost);

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).contains("transferToFactory");
    }

    @Test
    @DisplayName("sellBlock should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnSell() {
        Long blockId = 100L;

        BackEndResponse response = blockController.sellBlock(blockId, 9L, new BigDecimal("500000"));

        Mockito.verify(quarryBlockService).sellBlockExternally(
                org.mockito.ArgumentMatchers.eq(blockId),
                org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("500000")),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.isNull());
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Sell block successful");
    }

    @Test
    @DisplayName("sellBlock should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnSellError() {
        Long blockId = 100L;
        Mockito.doThrow(new RuntimeException("Lock conflict")).when(quarryBlockService)
                .sellBlockExternally(
                        org.mockito.ArgumentMatchers.eq(blockId),
                        org.mockito.ArgumentMatchers.eq(9L),
                        org.mockito.ArgumentMatchers.eq(new BigDecimal("500000")),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull());

        BackEndResponse response = blockController.sellBlock(blockId, 9L, new BigDecimal("500000"));

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).contains("sellBlock");
    }

    @Test
    @DisplayName("moveToYardApi should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnMove() {
        Long blockId = 100L;

        BackEndResponse response = blockController.moveToYardApi(
                blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        Mockito.verify(quarryBlockService).moveToYard(blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Move to yard successful");
    }

    @Test
    @DisplayName("moveToYardApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnMoveError() {
        Long blockId = 100L;
        Mockito.doThrow(new RuntimeException("Not at quarry")).when(quarryBlockService)
                .moveToYard(blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        BackEndResponse response = blockController.moveToYardApi(
                blockId, com.ozerler.marble.model.enums.StockLocationType.DISPATCH_YARD, "Stok sahası");

        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.ERR_FATAL);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage()).contains("moveToYard");
    }

    @Test
    @DisplayName("deleteBlock form action redirects to /blocks on success")
    void shouldRedirectToBlocksOnDeleteBlockFormSuccess() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Mockito.verify(quarryBlockService).deleteBlock(blockId);
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("deleteBlock form action redirects to /blocks/{id} on error")
    void shouldRedirectToBlockDetailOnDeleteBlockFormError() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.doThrow(new IllegalStateException("Kullanımda olan blok silinemez")).when(quarryBlockService).deleteBlock(blockId);
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
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

        Mockito.verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD, null, "A-BLOK");
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
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

        Mockito.verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-02", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds, "A3");
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
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

        Mockito.verify(quarryBlockService).updateBlock(
                10L, 1L, "BLK-UPD-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                new BigDecimal("5000"), "Not", null, StockLocationType.DISPATCH_YARD, fileIds, "A-BLOK");
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("showMoveForm opens dedicated page with requested target yard")
    void shouldOpenMoveFormForQuarryBlock() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.DISPATCH_YARD, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/move");
        Assertions.assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
        Assertions.assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showMoveForm defaults target to opposite quarry yard")
    void shouldDefaultMoveTargetToOppositeYard() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, null, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/move");
        Assertions.assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
    }

    @Test
    @DisplayName("showMoveForm redirects sold blocks to detail")
    void shouldRedirectSoldBlockAwayFromMoveForm() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.SOLD, StockLocationType.DISPATCH_YARD));
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.already_sold"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("already sold");
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.PRODUCTION_YARD, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("moveToYard form redirects to move page on error")
    void shouldRedirectToMovePageOnMoveFormError() {
        Mockito.doThrow(new IllegalArgumentException("Blok ocak sahasında değil")).when(quarryBlockService)
                .moveToYard(100L, StockLocationType.DISPATCH_YARD, "Stok sahası");
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("common.error.prefix"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Error: Blok ocak sahasında değil");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/move?targetType=DISPATCH_YARD");
    }

    @Test
    @DisplayName("showTransferForm opens dedicated page for dispatch-yard blocks")
    void shouldOpenTransferFormForDispatchYardBlock() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.DISPATCH_YARD));
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/transfer");
        Assertions.assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showTransferForm redirects production-yard blocks to detail")
    void shouldRedirectProductionYardBlockAwayFromTransferForm() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("error.block.dispatch.not_in_dispatch_yard"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("only dispatch");
        org.springframework.ui.Model model = new org.springframework.ui.ConcurrentModel();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("transferToFactory form redirects to transfer page on error")
    void shouldRedirectToTransferPageOnTransferFormError() {
        Mockito.doThrow(new IllegalArgumentException("only dispatch")).when(quarryBlockService)
                .transferToFactory(100L, new BigDecimal("12500"));
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("common.error.prefix"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Error: only dispatch");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/transfer-to-factory");
    }

    @Test
    @DisplayName("moveToYard form redirects to list when redirectTarget is list")
    void shouldRedirectToListOnMoveFormSuccessWhenTargetIsList() {
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.move.success"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Taşıma başarılı");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası", "list",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("moveToYard form redirects to detail when redirectTarget is detail")
    void shouldRedirectToDetailOnMoveFormSuccessWhenTargetIsDetail() {
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.move.success"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Taşıma başarılı");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası", "detail",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("transferToFactory form redirects to list when redirectTarget is list")
    void shouldRedirectToListOnTransferFormSuccessWhenTargetIsList() {
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.transfer.success"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Sevk başarılı");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), "list",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("transferToFactory form redirects to detail when redirectTarget is detail")
    void shouldRedirectToDetailOnTransferFormSuccessWhenTargetIsDetail() {
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.transfer.success"),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("Sevk başarılı");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), "detail",
                java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("blockPhotos redirects to the first image when photos exist")
    void blockPhotosRedirectsToFirstImage() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(java.util.List.of(com.ozerler.marble.dto.FileStorageDto.fromEntity(sampleImage(7L)), com.ozerler.marble.dto.FileStorageDto.fromEntity(sampleImage(8L))));
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.blockPhotos(100L, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/photos/7");
    }

    @Test
    @DisplayName("blockPhotos redirects to detail when the block has no images")
    void blockPhotosRedirectsWhenEmpty() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(java.util.List.of());
        Mockito.when(messageSource.getMessage(
                        org.mockito.ArgumentMatchers.eq("erp.block.photos.empty"),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn("no photos");
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.blockPhotos(100L, java.util.Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
        Assertions.assertThat(redirectAttributes.getFlashAttributes().get("warningMessage")).isEqualTo("no photos");
    }

    @Test
    @DisplayName("blockPhoto renders the dedicated photo page for a matching file")
    void blockPhotoRendersViewer() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                com.ozerler.marble.model.enums.BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(java.util.List.of(com.ozerler.marble.dto.FileStorageDto.fromEntity(sampleImage(7L)), com.ozerler.marble.dto.FileStorageDto.fromEntity(sampleImage(8L))));
        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes =
                new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = blockController.blockPhoto(
                100L, 8L, java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/photos");
        Assertions.assertThat(model.get("currentIndex")).isEqualTo(1);
        com.ozerler.marble.dto.FileStorageDto current =
                (com.ozerler.marble.dto.FileStorageDto) model.get("currentImage");
        Assertions.assertThat(current.getId()).isEqualTo(8L);
        Assertions.assertThat(model.get("previousImage")).isNotNull();
        Assertions.assertThat(model.get("nextImage")).isNotNull();
    }

    private static com.ozerler.marble.model.FileStorage sampleImage(Long id) {
        return com.ozerler.marble.model.FileStorage.builder()
                .id(id)
                .fileName("block-" + id + ".png")
                .originalName("cephe-" + id + ".png")
                .mimeType("image/png")
                .fileSize(2048L)
                .filePath("/media/block-" + id + ".png")
                .entityType("BLOCK")
                .entityId(100L)
                .build();
    }

    private static com.ozerler.marble.dto.view.BlockView blockView(Long id, String code) {
        com.ozerler.marble.dto.view.BlockView view = new com.ozerler.marble.dto.view.BlockView();
        view.setId(id);
        view.setBlockCode(code);
        return view;
    }

    private static com.ozerler.marble.dto.view.BlockView quarryBlock(
            com.ozerler.marble.model.enums.BlockStatus status,
            StockLocationType locationType) {
        com.ozerler.marble.dto.view.BlockView view = blockView(100L, "BLK-100");
        view.setStatus(status);
        view.setCanonicalStatus(status.canonical());
        if (locationType != null) {
            com.ozerler.marble.dto.view.BlockView.LocationView location = new com.ozerler.marble.dto.view.BlockView.LocationView();
            location.setName(locationType.name());
            location.setLocationType(locationType);
            view.setCurrentLocation(location);
        }
        return view;
    }
}
