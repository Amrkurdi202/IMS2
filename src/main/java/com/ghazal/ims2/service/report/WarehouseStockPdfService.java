package com.ghazal.ims2.service.report;

import com.ghazal.ims2.entity.Warehouse;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class WarehouseStockPdfService {

    public record StockRow(String itemName, String unitName, BigDecimal stockQty) {}

    private static final Pattern ARABIC = Pattern.compile(".*\\p{IsArabic}.*");
    private static final Color HEADER_BG = new Color(52, 120, 180);
    private static final Color ROW_ALT   = new Color(245, 247, 250);

    private boolean hasArabic(String text) {
        return text != null && !text.isEmpty() && ARABIC.matcher(text).matches();
    }

    private String norm(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFC).replace('\u00A0', ' ').trim();
    }

    private String fixArabic(String text) {
        text = norm(text);
        if (text == null || text.isEmpty()) return text;
        if (!hasArabic(text)) return text;
        try {
            ArabicShaping shaper = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE | ArabicShaping.LENGTH_GROW_SHRINK
                            | ArabicShaping.TEXT_DIRECTION_LOGICAL);
            return shaper.shape(text);
        } catch (ArabicShapingException e) {
            return text;
        }
    }

    private BaseFont loadArabicFont() throws DocumentException {
        for (String path : new String[]{"/fonts/NotoNaskhArabic-Regular.ttf", "/fonts/Amiri-Regular.ttf"}) {
            try (InputStream in = WarehouseStockPdfService.class.getResourceAsStream(path)) {
                if (in == null) continue;
                byte[] bytes = in.readAllBytes();
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
            } catch (Exception ignored) {}
        }
        throw new DocumentException("Arabic font missing — add NotoNaskhArabic-Regular.ttf to resources/fonts/");
    }

    public byte[] generatePdf(Warehouse warehouse, List<StockRow> rows) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        BaseFont bf = loadArabicFont();
        Font titleFont  = new Font(bf, 18, Font.BOLD);
        Font headerFont = new Font(bf, 12, Font.BOLD);
        Font normalFont = new Font(bf, 11, Font.NORMAL);
        Font smallFont  = new Font(bf, 9,  Font.NORMAL);

        // ── Title ──────────────────────────────────────────────────────
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingAfter(12);
        PdfPCell titleCell = new PdfPCell(new Phrase(fixArabic("تقرير مخزون المستودع"), titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(4);
        titleCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        titleTable.addCell(titleCell);
        document.add(titleTable);

        // ── Meta info ──────────────────────────────────────────────────
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(60);
        meta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        meta.setWidths(new float[]{2, 3});
        meta.setSpacingAfter(16);
        meta.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        addInfoRow(meta, "المستودع", warehouse.getName(), headerFont, normalFont);
        addInfoRow(meta, "التاريخ", LocalDate.now().toString(), headerFont, normalFont);
        addInfoRow(meta, "عدد الأصناف", String.valueOf(rows.size()), headerFont, normalFont);
        document.add(meta);

        // ── Items table ────────────────────────────────────────────────
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1f, 4f, 2f, 2f});
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.setSpacingBefore(4);

        Font whiteHeader = new Font(bf, 12, Font.BOLD, Color.WHITE);
        addTableHeader(table, "#",          whiteHeader);
        addTableHeader(table, "اسم الصنف",  whiteHeader);
        addTableHeader(table, "الوحدة",     whiteHeader);
        addTableHeader(table, "الكمية المتاحة", whiteHeader);

        for (int i = 0; i < rows.size(); i++) {
            StockRow row  = rows.get(i);
            Color    bg   = (i % 2 == 1) ? ROW_ALT : Color.WHITE;
            addTableCell(table, String.valueOf(i + 1),                    normalFont, Element.ALIGN_CENTER, bg);
            addTableCell(table, row.itemName(),                           normalFont,
                    hasArabic(row.itemName()) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, bg);
            addTableCell(table, row.unitName(),                           normalFont,
                    hasArabic(row.unitName()) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT, bg);
            addTableCell(table, row.stockQty().stripTrailingZeros().toPlainString(),
                    normalFont, Element.ALIGN_CENTER, bg);
        }
        document.add(table);

        // ── Footer ─────────────────────────────────────────────────────
        document.add(new Paragraph(" "));
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        PdfPCell fc = new PdfPCell(new Phrase(fixArabic("تم الإنشاء بواسطة نظام غزال"), smallFont));
        fc.setBorder(Rectangle.NO_BORDER);
        fc.setHorizontalAlignment(Element.ALIGN_CENTER);
        fc.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        footer.addCell(fc);
        document.add(footer);

        document.close();
        return baos.toByteArray();
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font lf, Font vf) {
        PdfPCell lc = new PdfPCell(new Phrase(fixArabic(label != null ? label : ""), lf));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPaddingBottom(5);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.addCell(lc);

        String v = value != null ? value : "";
        PdfPCell vc = new PdfPCell(new Phrase(fixArabic(v), vf));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPaddingBottom(5);
        vc.setHorizontalAlignment(hasArabic(v) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        vc.setRunDirection(hasArabic(v) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(vc);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        String t = text != null ? text : "";
        PdfPCell cell = new PdfPCell(new Phrase(fixArabic(t), font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        cell.setRunDirection(hasArabic(t) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align, Color bg) {
        String t = text != null ? text : "";
        PdfPCell cell = new PdfPCell(new Phrase(fixArabic(t), font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setBackgroundColor(bg);
        cell.setRunDirection(hasArabic(t) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(cell);
    }
}
