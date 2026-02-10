package com.ghazal.ims2.service;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
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
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Service("OrderReportService")
public class OrderReportService {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private FetchPlans fetchPlans;

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
        try (InputStream in = OrderReportService.class.getResourceAsStream(resourcePath)) {
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

    public byte[] generateOrderPdf(Order order) throws DocumentException, IOException {
        FetchPlan fetchPlan = fetchPlans.builder(Order.class)
                .addFetchPlan(FetchPlan.BASE)
                .add("warehouse", FetchPlan.BASE)
                .add("project", FetchPlan.BASE)
                .add("user", FetchPlan.BASE)
                .add("items", fp -> fp
                        .addFetchPlan(FetchPlan.BASE)
                        .add("item", FetchPlan.BASE)
                        .add("unit", FetchPlan.BASE))
                .build();

        order = dataManager.load(Order.class)
                .id(order.getId())
                .fetchPlan(fetchPlan)
                .one();

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);

        document.open();

        BaseFont arabicFont = loadArabicBaseFont();

        Font titleFont = new Font(arabicFont, 18, Font.BOLD);
        Font headerFont = new Font(arabicFont, 14, Font.BOLD);
        Font normalFont = new Font(arabicFont, 12, Font.NORMAL);
        Font smallFont = new Font(arabicFont, 10, Font.NORMAL);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell titleCell = new PdfPCell(new Phrase(fixArabic("تقرير الطلب"), titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setPaddingBottom(20);
        titleCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        titleTable.addCell(titleCell);

        document.add(titleTable);

        addSectionHeader(document, "معلومات الطلب", headerFont);
        document.add(new Paragraph(" "));

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(10);
        infoTable.setSpacingAfter(20);
        infoTable.setWidths(new float[]{2, 3});
        infoTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        addInfoRow(infoTable, "رقم الطلب", order.getId() != null ? order.getId().toString() : "N A", headerFont, normalFont);
        addInfoRow(infoTable, "المستودع", order.getWarehouse() != null ? order.getWarehouse().getName() : "N A", headerFont, normalFont);
        addInfoRow(infoTable, "المشروع", order.getProject() != null ? order.getProject().getName() : "N A", headerFont, normalFont);

        String userName = order.getUser() != null ? order.getUser().getDisplayName() : "N A";
        addInfoRow(infoTable, "المستخدم", userName, headerFont, normalFont);

        if (order.getCreatedDate() != null) {
            String dateStr = order.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyyMMdd HH:mm"));
            addInfoRow(infoTable, "التاريخ", dateStr, headerFont, normalFont);
        }

        document.add(infoTable);

        addSectionHeader(document, "عناصر الطلب", headerFont);
        document.add(new Paragraph(" "));

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            PdfPTable itemsTable = new PdfPTable(5);
            itemsTable.setWidthPercentage(100);
            itemsTable.setSpacingBefore(10);
            itemsTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            itemsTable.setWidths(new float[]{2, 2, 2, 3, 1});

            addTableHeader(itemsTable, "السعر", headerFont);
            addTableHeader(itemsTable, "الكمية", headerFont);
            addTableHeader(itemsTable, "الوحدة", headerFont);
            addTableHeader(itemsTable, "الصنف", headerFont);
            addTableHeader(itemsTable, "", headerFont);

            int rowNum = 1;
            BigDecimal totalPrice = BigDecimal.ZERO;

            for (OrderItem item : order.getItems()) {
                String price = item.getUnitPriceAtOrder() != null ? item.getUnitPriceAtOrder().toString() : "0.00";
                addTableCell(itemsTable, price, normalFont, Element.ALIGN_CENTER);

                String quantity = item.getQuantity() != null ? item.getQuantity().toString() : "0";
                addTableCell(itemsTable, quantity, normalFont, Element.ALIGN_CENTER);

                String unitName = item.getUnit() != null ? item.getUnit().getName() : "N A";
                addTableCell(itemsTable, unitName, normalFont, hasArabic(unitName) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

                String itemName;
                if (Boolean.TRUE.equals(item.getCustomItem())) {
                    itemName = item.getCustomName() != null ? item.getCustomName() : "Custom Item";
                } else if (item.getItem() != null) {
                    itemName = item.getItem().getName();
                } else {
                    itemName = "N A";
                }
                addTableCell(itemsTable, itemName, normalFont, hasArabic(itemName) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);

                addTableCell(itemsTable, String.valueOf(rowNum++), normalFont, Element.ALIGN_CENTER);

                if (item.getQuantity() != null && item.getUnitPriceAtOrder() != null) {
                    totalPrice = totalPrice.add(item.getQuantity().multiply(item.getUnitPriceAtOrder()));
                }
            }

            PdfPCell totalValueCell = new PdfPCell(new Phrase(totalPrice.toString(), headerFont));
            totalValueCell.setBackgroundColor(new Color(240, 240, 240));
            totalValueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalValueCell.setPadding(6);
            totalValueCell.setRunDirection(PdfWriter.RUN_DIRECTION_LTR);
            itemsTable.addCell(totalValueCell);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase(fixArabic("الإجمالي"), headerFont));
            totalLabelCell.setColspan(4);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setBackgroundColor(new Color(240, 240, 240));
            totalLabelCell.setPadding(6);
            totalLabelCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            itemsTable.addCell(totalLabelCell);

            document.add(itemsTable);
        } else {
            PdfPTable noItemsTable = new PdfPTable(1);
            noItemsTable.setWidthPercentage(100);

            PdfPCell noItemsCell = new PdfPCell(new Phrase(fixArabic("لا توجد عناصر في هذا الطلب"), normalFont));
            noItemsCell.setBorder(Rectangle.NO_BORDER);
            noItemsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            noItemsCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
            noItemsTable.addCell(noItemsCell);

            document.add(noItemsTable);
        }

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        PdfPCell footerCell = new PdfPCell(new Phrase(fixArabic("تم الإنشاء بواسطة نظام غزال"), smallFont));
        footerCell.setBorder(Rectangle.NO_BORDER);
        footerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        footerCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        footerTable.addCell(footerCell);

        document.add(footerTable);

        document.close();
        return baos.toByteArray();
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
}
