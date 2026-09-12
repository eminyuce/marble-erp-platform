package com.ozerler.marble.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ozerler.marble.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

/**
 * Generates QR code images as Base64-encoded PNGs using ZXing.
 */
@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class BarcodeService {

    private final org.springframework.context.MessageSource messageSource;

    private String getMessage(String code, Object... args) {
        if (messageSource != null) {
            try {
                return messageSource.getMessage(code, args, org.springframework.context.i18n.LocaleContextHolder.getLocale());
            } catch (Exception ignored) {
            }
        }
        return com.ozerler.marble.util.MessageUtils.getMessage(code, args);
    }

    /**
     * Generates a QR code image from the given text and returns it as a
     * Base64-encoded PNG string suitable for embedding in an HTML {@code <img>} tag.
     *
     * @param text   the content to encode (e.g. a URL or slab code)
     * @param width  pixel width of the QR image
     * @param height pixel height of the QR image
     * @return Base64-encoded PNG string
     */
    public String generateQrCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 1,
                    EncodeHintType.CHARACTER_SET, "UTF-8"
            );

            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("QR code generation failed for text: {}", text, e);
            throw new RuntimeException(getMessage("error.barcode.qr_failed"), e);
        }
    }

    /**
     * Convenience overload using the default 250×250 size.
     */
    public String generateQrCodeBase64(String text) {
        return generateQrCodeBase64(text, Constants.DEFAULT_QR_CODE_SIZE, Constants.DEFAULT_QR_CODE_SIZE);
    }
}
