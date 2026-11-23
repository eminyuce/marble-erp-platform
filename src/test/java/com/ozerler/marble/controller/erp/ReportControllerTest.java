package com.ozerler.marble.controller.erp;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ozerler.marble.service.ReportService;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final Pattern CONTENT_DISPOSITION_FILENAME =
            Pattern.compile("filename=\"([^\"]+)\"");
    private static final Pattern EXPORT_FILENAME =
            Pattern.compile("^[a-z0-9_-]+_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.(csv|xlsx)$");

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    @DisplayName("CSV export keeps report bytes unchanged and uses a timestamped Turkish filename")
    void csvExportKeepsBodyAndUsesConvention() throws IOException {
        byte[] csvBytes = "blok;adet\nB-1;3".getBytes();
        Mockito.when(reportService.generateCsvReport(ReportService.ReportType.QUARRY_BLOCKS)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = reportController.exportReport("QUARRY_BLOCKS", "csv");

        Mockito.verify(reportService).generateCsvReport(ReportService.ReportType.QUARRY_BLOCKS);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(csvBytes);
        Assertions.assertThat(response.getHeaders().getContentType()).isNotNull();
        Assertions.assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        assertFilename(response, "ocak_bloklari", "csv");
    }

    @Test
    @DisplayName("Excel export keeps report bytes unchanged and uses a timestamped Turkish filename")
    void excelExportKeepsBodyAndUsesConvention() throws IOException {
        byte[] excelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        Mockito.when(reportService.generateExcelReport(ReportService.ReportType.COST_ACCOUNTING))
                .thenReturn(excelBytes);

        ResponseEntity<byte[]> response = reportController.exportReport("cost_accounting", "excel");

        Mockito.verify(reportService).generateExcelReport(ReportService.ReportType.COST_ACCOUNTING);
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(response.getBody()).isEqualTo(excelBytes);
        assertFilename(response, "maliyet_raporu", "xlsx");
    }

    @Test
    @DisplayName("Unknown report type is rejected without generating a file")
    void unknownReportTypeIsRejected() throws IOException {
        ResponseEntity<byte[]> response = reportController.exportReport("unknown", "csv");

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Assertions.assertThat(response.getBody()).isNull();
        Mockito.verifyNoInteractions(reportService);
    }

    private static void assertFilename(ResponseEntity<byte[]> response, String entity, String extension) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        Assertions.assertThat(disposition).isNotBlank();

        Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(disposition);
        Assertions.assertThat(matcher.find()).isTrue();
        String filename = matcher.group(1);

        Assertions.assertThat(filename).matches(EXPORT_FILENAME);
        Assertions.assertThat(filename).startsWith(entity + "_");
        Assertions.assertThat(filename).endsWith("." + extension);
        Assertions.assertThat(filename).doesNotContain(":", " ", "ş", "ı", "ğ", "ü", "ö", "ç");
    }
}
