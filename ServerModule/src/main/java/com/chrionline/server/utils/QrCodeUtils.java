package com.chrionline.server.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

public class QrCodeUtils {

    /**
     * Convertit une OTP URL en image QR Code encodée en Base64 (PNG).
     * Le frontend peut l'afficher directement avec : <img src="data:image/png;base64,..." />
     */
    public static String toBase64Png(String otpUrl, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2
            );

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpUrl, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            return Base64.getEncoder().encodeToString(out.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("Erreur génération QR Code", e);
        }
    }
}
