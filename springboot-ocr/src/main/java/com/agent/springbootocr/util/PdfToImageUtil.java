package com.agent.springbootocr.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.*;

public class PdfToImageUtil {

    /**
     * 将 PDF 每页转为 Base64 编码的 PNG 图片列表
     */
    public static List<String> convertToBase64Images(File pdfFile, float dpi) throws Exception {
        List<String> base64List = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                base64List.add(base64);
            }
        }
        return base64List;
    }
}
