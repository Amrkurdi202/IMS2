package com.ghazal.ims2.service.report;

import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Warehouse;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ProjectCostReportPdfService {

    private static final Pattern ARABIC = Pattern.compile(".*\\p{IsArabic}.*");

    private boolean hasArabic(String text) {
        return text != null && !text.isEmpty() && ARABIC.matcher(text).matches();
    }

    private String norm(String text) {
        if (text == null) return null;
        return Normalizer.normalize(text, Normalizer.Form.NFC).replace('\u00A0', ' ').trim();
    }

    private String fixArabic(String text) {
        text = norm(text);
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (!hasArabic(text)) {
            return text;
        }
        try {
            ArabicShaping shaper = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE
                            | ArabicShaping.LENGTH_GROW_SHRINK
                            | ArabicShaping.TEXT_DIRECTION_LOGICAL
            );
            return shaper.shape(text);
        } catch (ArabicShapingException e) {
            return text;
        }
    }

    private BaseFont loadArabicBaseFont() throws DocumentException {
        BaseFont bf = tryLoadFromClasspath("/fonts/NotoNaskhArabic-Regular.ttf");
        if (bf != null) return bf;

        bf = tryLoadFromClasspath("/fonts/Amiri-Regular.ttf");
        if (bf != null) return bf;

        throw new DocumentException("arabic font missing add NotoNaskhArabic Regular ttf to src main resources fonts");
    }

    private BaseFont tryLoadFromClasspath(String resourcePath) {
        try (InputStream in = ProjectCostReportPdfService.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(resourcePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void addSectionHeader(Document document, String text, Font font) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell c = new PdfPCell(new Phrase(fixArabic(text), font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPaddingTop(6);
        c.setPaddingBottom(6);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        t.addCell(c);

        document.add(t);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        String l = label == null ? "" : label;
        String v = value == null ? "" : value;

        PdfPCell labelCell = new PdfPCell(new Phrase(fixArabic(l), labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(6);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(fixArabic(v), valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(6);
        valueCell.setHorizontalAlignment(hasArabic(v) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
        valueCell.setRunDirection(hasArabic(v) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        String t = text == null ? "" : text;

        PdfPCell cell = new PdfPCell(new Phrase(fixArabic(t), font));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setRunDirection(hasArabic(t) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int align) {
        String t = text == null ? "" : text;

        PdfPCell cell = new PdfPCell(new Phrase(fixArabic(t), font));
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        cell.setRunDirection(hasArabic(t) ? PdfWriter.RUN_DIRECTION_RTL : PdfWriter.RUN_DIRECTION_LTR);
        table.addCell(cell);
    }

    private String itemDisplayName(OrderItem item) {
        if (item == null) return "";
        String cn = item.getCustomName();
        if (cn != null && !cn.isBlank()) return cn;
        if (item.getItem() != null && item.getItem().getName() != null) return item.getItem().getName();
        return "";
    }

    public byte[] generateProjectCostPdf(
            Project project,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Warehouse warehouse,
            BigDecimal pointValue,
            ProjectCostReportResult result,
            List<OrderItem> items
    ) throws DocumentException {

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        BaseFont arabicFont = loadArabicBaseFont();

        Font titleFont = new Font(arabicFont, 18, Font.BOLD);
        Font headerFont = new Font(arabicFont, 14, Font.BOLD);
        Font normalFont = new Font(arabicFont, 12, Font.NORMAL);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell(new Phrase(fixArabic("تقرير تكلفة المشروع"), titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(18);
        titleCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        titleTable.addCell(titleCell);

        document.add(titleTable);

        addSectionHeader(document, "معلومات التقرير", headerFont);
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(18);
        infoTable.setWidths(new float[]{2, 3});
        infoTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        addInfoRow(infoTable, "المشروع", project != null ? project.getName() : "N A", headerFont, normalFont);
        addInfoRow(infoTable, "المستودع", warehouse != null ? warehouse.getName() : "N A", headerFont, normalFont);

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        addInfoRow(infoTable, "من", fromDate != null ? fromDate.toLocalDate().format(df) : "N A", headerFont, normalFont);
        addInfoRow(infoTable, "الى", toDate != null ? toDate.toLocalDate().format(df) : "N A", headerFont, normalFont);

        addInfoRow(infoTable, "قيمة النقطة", pointValue != null ? pointValue.toString() : "0", headerFont, normalFont);

        addInfoRow(infoTable, "اجمالي النقاط", result != null && result.totalPoints() != null ? result.totalPoints().toString() : "0", headerFont, normalFont);
        addInfoRow(infoTable, "تكلفة النقاط", result != null && result.pointsCost() != null ? result.pointsCost().toString() : "0", headerFont, normalFont);
        addInfoRow(infoTable, "مبلغ العميل", result != null && result.clientAmount() != null ? result.clientAmount().toString() : "0", headerFont, normalFont);

        document.add(infoTable);

        addSectionHeader(document, "عناصر الطلب", headerFont);
        document.add(new Paragraph(" "));

        if (items != null && !items.isEmpty()) {
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingBefore(10);
            itemsTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            itemsTable.setWidths(new float[]{2, 2, 2, 4});

            addTableHeader(itemsTable, "السعر", headerFont);
            addTableHeader(itemsTable, "الكمية", headerFont);
            addTableHeader(itemsTable, "الوحدة", headerFont);
            addTableHeader(itemsTable, "الصنف", headerFont);

            for (OrderItem item : items) {
                String price = item.getUnitPriceAtOrder() != null ? item.getUnitPriceAtOrder().toString() : "0.00";
                addTableCell(itemsTable, price, normalFont, Element.ALIGN_CENTER);

                String quantity = item.getQuantity() != null ? item.getQuantity().toString() : "0";
                addTableCell(itemsTable, quantity, normalFont, Element.ALIGN_CENTER);

                String unitName = item.getUnit() != null ? item.getUnit().getName() : "N A";
                addTableCell(itemsTable, unitName, normalFont, hasArabic(unitName) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

                String name = itemDisplayName(item);
                addTableCell(itemsTable, name, normalFont, hasArabic(name) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            }

            document.add(itemsTable);
        } else {
            PdfPTable noItemsTable = new PdfPTable(1);
            noItemsTable.setWidthPercentage(100);

            PdfPCell noItemsCell = new PdfPCell(new Phrase(fixArabic("لا توجد عناصر"), normalFont));
            noItemsCell.setBorder(Rectangle.NO_BORDER);
            noItemsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            noItemsCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            noItemsTable.addCell(noItemsCell);

            document.add(noItemsTable);
        }

        document.close();
        return baos.toByteArray();
    }
}
