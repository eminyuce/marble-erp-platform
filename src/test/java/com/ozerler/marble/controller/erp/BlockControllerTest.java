package com.ozerler.marble.controller.erp;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ozerler.marble.common.Constants;
import com.ozerler.marble.dto.FileStorageDto;
import com.ozerler.marble.dto.view.BlockView;
import com.ozerler.marble.model.FileStorage;
import com.ozerler.marble.model.enums.BlockStatus;
import com.ozerler.marble.model.enums.QualityGrade;
import com.ozerler.marble.model.enums.StockLocationType;
import com.ozerler.marble.model.response.BackEndResponse;
import com.ozerler.marble.service.FileStorageService;
import com.ozerler.marble.service.QuarryBlockService;

@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    @Mock
    private QuarryBlockService quarryBlockService;

    @Mock
    private MessageSource messageSource;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private BlockController blockController;

    @Test
    @DisplayName("deleteBlockApi should return successful BackEndResponse")
    void shouldReturnSuccessBackEndResponseOnDeleteBlockApi() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.when(
                        messageSource.getMessage(
                                ArgumentMatchers.eq("erp.block.delete.success"),
                                ArgumentMatchers.any(),
                                ArgumentMatchers.any()))
                .thenReturn("BLK-100 kodlu blok silindi.");

        BackEndResponse response = blockController.deleteBlockApi(blockId, Locale.forLanguageTag("tr"));

        Mockito.verify(quarryBlockService).deleteBlock(blockId);
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage())
                .isEqualTo("BLK-100 kodlu blok silindi.");
    }

    @Test
    @DisplayName("deleteBlockApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnDeleteBlockApiError() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.doThrow(new IllegalStateException("Blok silinemez")).when(quarryBlockService).deleteBlock(blockId);

        BackEndResponse response = blockController.deleteBlockApi(blockId, Locale.forLanguageTag("tr"));

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
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage())
                .isEqualTo("Transfer to factory successful");
    }

    @Test
    @DisplayName("transferToFactory should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnTransferError() {
        Long blockId = 100L;
        BigDecimal transportCost = new BigDecimal("1500.00");
        Mockito.doThrow(new RuntimeException("Database error"))
                .when(quarryBlockService)
                .transferToFactory(blockId, transportCost);

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
                ArgumentMatchers.eq(blockId),
                ArgumentMatchers.eq(9L),
                ArgumentMatchers.eq(new BigDecimal("500000")),
                ArgumentMatchers.any(),
                ArgumentMatchers.isNull());
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
                        ArgumentMatchers.eq(blockId),
                        ArgumentMatchers.eq(9L),
                        ArgumentMatchers.eq(new BigDecimal("500000")),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.isNull());

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
                blockId, StockLocationType.DISPATCH_YARD, "Stok sahası");

        Mockito.verify(quarryBlockService).moveToYard(blockId, StockLocationType.DISPATCH_YARD, "Stok sahası");
        Assertions.assertThat(response).isNotNull();
        Assertions.assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        Assertions.assertThat(response.getServiceStatus().getStatus().getMessage())
                .isEqualTo("Move to yard successful");
    }

    @Test
    @DisplayName("moveToYardApi should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnMoveError() {
        Long blockId = 100L;
        Mockito.doThrow(new RuntimeException("Not at quarry")).when(quarryBlockService)
                .moveToYard(blockId, StockLocationType.DISPATCH_YARD, "Stok sahası");

        BackEndResponse response = blockController.moveToYardApi(
                blockId, StockLocationType.DISPATCH_YARD, "Stok sahası");

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
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, Locale.forLanguageTag("tr"), redirectAttributes);

        Mockito.verify(quarryBlockService).deleteBlock(blockId);
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("deleteBlock form action redirects to /blocks/{id} on error")
    void shouldRedirectToBlockDetailOnDeleteBlockFormError() {
        Long blockId = 100L;
        Mockito.when(quarryBlockService.getBlock(blockId)).thenReturn(blockView(blockId, "BLK-100"));
        Mockito.doThrow(new IllegalStateException("Kullanımda olan blok silinemez"))
                .when(quarryBlockService)
                .deleteBlock(blockId);
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.deleteBlock(blockId, Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("createBlock passes locationType to quarryBlockService.registerBlock")
    void shouldPassLocationTypeToRegisterBlock() {
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();

        String view = blockController.createBlock(
                1L, "BLK-NEW-01", StockLocationType.DISPATCH_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, null, "A-BLOK",
                Locale.forLanguageTag("tr"), model, redirectAttributes);

        Mockito.verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD, null, "A-BLOK");
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("createBlock passes fileIds to quarryBlockService.registerBlock")
    void shouldPassFileIdsToRegisterBlock() {
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();
        List<Long> fileIds = List.of(55L, 56L);

        String view = blockController.createBlock(
                1L, "BLK-NEW-02", StockLocationType.PRODUCTION_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, fileIds, "A3",
                Locale.forLanguageTag("tr"), model, redirectAttributes);

        Mockito.verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-02", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds, "A3");
        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("updateBlock passes locationType and fileIds to quarryBlockService.updateBlock")
    void shouldPassLocationTypeToUpdateBlock() {
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();
        Model model = new ConcurrentModel();
        List<Long> fileIds = List.of(77L);

        String view = blockController.updateBlock(
                10L, 1L, "BLK-UPD-01", StockLocationType.DISPATCH_YARD, null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                new BigDecimal("5000"), "Not", null, fileIds, "A-BLOK",
                Locale.forLanguageTag("tr"), model, redirectAttributes);

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
                .thenReturn(quarryBlock(BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.DISPATCH_YARD, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/move");
        Assertions.assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
        Assertions.assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showMoveForm defaults target to opposite quarry yard")
    void shouldDefaultMoveTargetToOppositeYard() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, null, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/move");
        Assertions.assertThat(model.getAttribute("selectedTargetType")).isEqualTo(StockLocationType.DISPATCH_YARD);
    }

    @Test
    @DisplayName("showMoveForm redirects sold blocks to detail")
    void shouldRedirectSoldBlockAwayFromMoveForm() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(BlockStatus.SOLD, StockLocationType.DISPATCH_YARD));
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.already_sold"),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenReturn("already sold");
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.showMoveForm(
                100L, StockLocationType.PRODUCTION_YARD, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("moveToYard form redirects to move page on error")
    void shouldRedirectToMovePageOnMoveFormError() {
        Mockito.doThrow(new IllegalArgumentException("Blok ocak sahasında değil")).when(quarryBlockService)
                .moveToYard(100L, StockLocationType.DISPATCH_YARD, "Stok sahası");
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("common.error.prefix"),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenReturn("Error: Blok ocak sahasında değil");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası",
                Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/move?targetType=DISPATCH_YARD");
    }

    @Test
    @DisplayName("showTransferForm opens dedicated page for dispatch-yard blocks")
    void shouldOpenTransferFormForDispatchYardBlock() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(BlockStatus.PRODUCED, StockLocationType.DISPATCH_YARD));
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/transfer");
        Assertions.assertThat(model.getAttribute("block")).isNotNull();
    }

    @Test
    @DisplayName("showTransferForm redirects production-yard blocks to detail")
    void shouldRedirectProductionYardBlockAwayFromTransferForm() {
        Mockito.when(quarryBlockService.getBlock(100L))
                .thenReturn(quarryBlock(BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("error.block.dispatch.not_in_dispatch_yard"),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.any()))
                .thenReturn("only dispatch");
        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.showTransferForm(
                100L, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("transferToFactory form redirects to transfer page on error")
    void shouldRedirectToTransferPageOnTransferFormError() {
        Mockito.doThrow(new IllegalArgumentException("only dispatch")).when(quarryBlockService)
                .transferToFactory(100L, new BigDecimal("12500"));
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("common.error.prefix"),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenReturn("Error: only dispatch");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/transfer-to-factory");
    }

    @Test
    @DisplayName("moveToYard form redirects to list when redirectTarget is list")
    void shouldRedirectToListOnMoveFormSuccessWhenTargetIsList() {
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.move.success"),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.any()))
                .thenReturn("Taşıma başarılı");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası", "list",
                Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("moveToYard form redirects to detail when redirectTarget is detail")
    void shouldRedirectToDetailOnMoveFormSuccessWhenTargetIsDetail() {
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.move.success"),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.any()))
                .thenReturn("Taşıma başarılı");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.moveToYard(
                100L, StockLocationType.DISPATCH_YARD, "Stok sahası", "detail",
                Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("transferToFactory form redirects to list when redirectTarget is list")
    void shouldRedirectToListOnTransferFormSuccessWhenTargetIsList() {
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.transfer.success"),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.any()))
                .thenReturn("Sevk başarılı");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), "list",
                Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks");
    }

    @Test
    @DisplayName("transferToFactory form redirects to detail when redirectTarget is detail")
    void shouldRedirectToDetailOnTransferFormSuccessWhenTargetIsDetail() {
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.transfer.success"),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.any()))
                .thenReturn("Sevk başarılı");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.transferToFactoryForm(
                100L, new BigDecimal("12500"), "detail",
                Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
    }

    @Test
    @DisplayName("blockPhotos redirects to the first image when photos exist")
    void blockPhotosRedirectsToFirstImage() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(
                        List.of(
                                FileStorageDto.fromEntity(sampleImage(7L)),
                                FileStorageDto.fromEntity(sampleImage(8L))));
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.blockPhotos(100L, Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100/photos/7");
    }

    @Test
    @DisplayName("blockPhotos redirects to detail when the block has no images")
    void blockPhotosRedirectsWhenEmpty() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(List.of());
        Mockito.when(messageSource.getMessage(
                        ArgumentMatchers.eq("erp.block.photos.empty"),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any()))
                .thenReturn("no photos");
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.blockPhotos(100L, Locale.forLanguageTag("tr"), redirectAttributes);

        Assertions.assertThat(view).isEqualTo("redirect:/blocks/100");
        Assertions.assertThat(redirectAttributes.getFlashAttributes().get("warningMessage")).isEqualTo("no photos");
    }

    @Test
    @DisplayName("blockPhoto renders the dedicated photo page for a matching file")
    void blockPhotoRendersViewer() {
        Mockito.when(quarryBlockService.getBlock(100L)).thenReturn(quarryBlock(
                BlockStatus.PRODUCED, StockLocationType.PRODUCTION_YARD));
        Mockito.when(fileStorageService.getFileViewsForEntity("BLOCK", 100L))
                .thenReturn(
                        List.of(
                                FileStorageDto.fromEntity(sampleImage(7L)),
                                FileStorageDto.fromEntity(sampleImage(8L))));
        ExtendedModelMap model = new ExtendedModelMap();
        RedirectAttributes redirectAttributes =
                new RedirectAttributesModelMap();

        String view = blockController.blockPhoto(
                100L, 8L, Locale.forLanguageTag("tr"), model, redirectAttributes);

        Assertions.assertThat(view).isEqualTo("erp/blocks/photos");
        Assertions.assertThat(model.get("currentIndex")).isEqualTo(1);
        FileStorageDto current =
                (FileStorageDto) model.get("currentImage");
        Assertions.assertThat(current.getId()).isEqualTo(8L);
        Assertions.assertThat(model.get("previousImage")).isNotNull();
        Assertions.assertThat(model.get("nextImage")).isNotNull();
    }

    private static FileStorage sampleImage(Long id) {
        return FileStorage.builder()
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

    private static BlockView blockView(Long id, String code) {
        BlockView view = new BlockView();
        view.setId(id);
        view.setBlockCode(code);
        return view;
    }

    private static BlockView quarryBlock(
            BlockStatus status,
            StockLocationType locationType) {
        BlockView view = blockView(100L, "BLK-100");
        view.setStatus(status);
        view.setCanonicalStatus(status.canonical());
        if (locationType != null) {
            BlockView.LocationView location = new BlockView.LocationView();
            location.setName(locationType.name());
            location.setLocationType(locationType);
            view.setCurrentLocation(location);
        }
        return view;
    }
}
