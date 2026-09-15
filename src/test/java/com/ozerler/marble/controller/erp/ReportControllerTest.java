package com.ozerler.marble.controller.erp;

import com.ozerler.marble.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
        when(reportService.generateCsvReport(ReportService.ReportType.QUARRY_BLOCKS)).thenReturn(csvBytes);

        ResponseEntity<byte[]> response = reportController.exportReport("QUARRY_BLOCKS", "csv");

        verify(reportService).generateCsvReport(ReportService.ReportType.QUARRY_BLOCKS);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(csvBytes);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        assertFilename(response, "ocak_bloklari", "csv");
    }

    @Test
    @DisplayName("Excel export keeps report bytes unchanged and uses a timestamped Turkish filename")
    void excelExportKeepsBodyAndUsesConvention() throws IOException {
        byte[] excelBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(reportService.generateExcelReport(ReportService.ReportType.COST_ACCOUNTING)).thenReturn(excelBytes);

        ResponseEntity<byte[]> response = reportController.exportReport("cost_accounting", "excel");

        verify(reportService).generateExcelReport(ReportService.ReportType.COST_ACCOUNTING);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(excelBytes);
        assertFilename(response, "maliyet_raporu", "xlsx");
    }

    @Test
    @DisplayName("Unknown report type is rejected without generating a file")
    void unknownReportTypeIsRejected() throws IOException {
        ResponseEntity<byte[]> response = reportController.exportReport("unknown", "csv");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
        verifyNoInteractions(reportService);
    }

    private static void assertFilename(ResponseEntity<byte[]> response, String entity, String extension) {
        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(disposition).isNotBlank();

        Matcher matcher = CONTENT_DISPOSITION_FILENAME.matcher(disposition);
        assertThat(matcher.find()).isTrue();
        String filename = matcher.group(1);

        assertThat(filename).matches(EXPORT_FILENAME);
        assertThat(filename).startsWith(entity + "_");
        assertThat(filename).endsWith("." + extension);
        assertThat(filename).doesNotContain(":", " ", "ş", "ı", "ğ", "ü", "ö", "ç");
    }
}
