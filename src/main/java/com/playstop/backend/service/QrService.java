package com.playstop.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class QrService {

    private static final int SIZE = 300;
    // Dark navy (#0f172a) on white — matches PlayStop brand
    private static final int COLOR_DARK  = 0xFF0f172a;
    private static final int COLOR_LIGHT = 0xFFffffff;

    public byte[] generateQr(String content) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(
            content,
            BarcodeFormat.QR_CODE,
            SIZE, SIZE,
            Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.CHARACTER_SET, "UTF-8")
        );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out,
            new MatrixToImageConfig(COLOR_DARK, COLOR_LIGHT));
        return out.toByteArray();
    }
}
