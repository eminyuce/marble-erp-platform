package com.ozerler.marble.controller.erp;

import com.ozerler.marble.common.Constants;
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

    @InjectMocks
    private BlockController blockController;

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

        BackEndResponse response = blockController.sellBlock(blockId, 9L);

        verify(quarryBlockService).sellBlockExternally(blockId, 9L);
        assertThat(response).isNotNull();
        assertThat(response.getServiceStatus().getHttpStatus()).isEqualTo(HttpStatus.OK);
        assertThat(response.getServiceStatus().getStatus().getErrorCode()).isEqualTo(Constants.NO_ERR);
        assertThat(response.getServiceStatus().getStatus().getMessage()).isEqualTo("Sell block successful");
    }

    @Test
    @DisplayName("sellBlock should return fatal BackEndResponse on exception")
    void shouldReturnFatalBackEndResponseOnSellError() {
        Long blockId = 100L;
        doThrow(new RuntimeException("Lock conflict")).when(quarryBlockService).sellBlockExternally(blockId, 9L);

        BackEndResponse response = blockController.sellBlock(blockId, 9L);

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
}
