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
                "Not", null, null,
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.DISPATCH_YARD, null);
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
                "Not", null, fileIds,
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).registerBlock(
                1L, "BLK-NEW-02", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                "Not", null, StockLocationType.PRODUCTION_YARD, fileIds);
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
                new BigDecimal("5000"), "Not", null, fileIds,
                java.util.Locale.forLanguageTag("tr"), model, redirectAttributes);

        verify(quarryBlockService).updateBlock(
                10L, 1L, "BLK-UPD-01", null, 150, 250, 140,
                new BigDecimal("14500"), "Muğla Beyaz", "Beyaz", QualityGrade.A, 0,
                new BigDecimal("5000"), "Not", null, StockLocationType.DISPATCH_YARD, fileIds);
        assertThat(view).isEqualTo("redirect:/blocks");
    }
}
