package com.domu.service;

import com.domu.dto.CommonChargeDetailResponse;
import com.domu.dto.CommonExpensePeriodDetailResponse;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfGState;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CommonExpensePdfService {

    private static final Color DOMU_YELLOW = new Color(247, 206, 15);
    private static final Color DOMU_ORANGE = new Color(241, 107, 50);
    private static final Color TEXT_DARK = new Color(20, 20, 20);
    private static final Color TEXT_MUTED = new Color(90, 90, 90);

    public byte[] buildResidentPeriodPdf(CommonExpensePeriodDetailResponse detail) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 48, 42);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.open();
            addWatermark(writer);
            addBanner(document, detail);
            addSummary(document, detail);
            addChargesTable(document, detail);
            addTotals(document, detail);
            addRevisions(document, detail);
            addFooter(document);
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new ValidationException("No se pudo generar el PDF de gastos comunes: " + e.getMessage());
        }
    }

    private void addWatermark(PdfWriter writer) {
        try {
            PdfContentByte canvas = writer.getDirectContentUnder();
            PdfGState state = new PdfGState();
            state.setFillOpacity(0.07f);
            canvas.setGState(state);
            BaseFont font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
            canvas.setFontAndSize(font, 72);
            canvas.setColorFill(new Color(200, 200, 200));
            canvas.showTextAligned(Element.ALIGN_CENTER, "DOMU",
                    PageSize.A4.getWidth() / 2, PageSize.A4.getHeight() / 2, 45);
        } catch (Exception ignored) {
            // Si falla el watermark, seguimos con el PDF base.
        }
    }

    private void addBanner(Document document, CommonExpensePeriodDetailResponse detail) throws Exception {
        Font title = new Font(Font.HELVETICA, 15, Font.BOLD, TEXT_DARK);
        Font subtitle = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_MUTED);

        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(DOMU_YELLOW);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.addElement(new Phrase("DOMU · Detalle de gasto común", title));
        String periodLabel = formatPeriod(detail.year(), detail.month());
        cell.addElement(new Phrase("Período " + periodLabel, subtitle));
        banner.addCell(cell);
        document.add(banner);

        PdfPTable accent = new PdfPTable(1);
        accent.setWidthPercentage(100);
        PdfPCell accentCell = new PdfPCell();
        accentCell.setBackgroundColor(DOMU_ORANGE);
        accentCell.setFixedHeight(3);
        accentCell.setBorder(Rectangle.NO_BORDER);
        accent.addCell(accentCell);
        document.add(accent);

        document.add(new Paragraph(" "));
    }

    private void addSummary(Document document, CommonExpensePeriodDetailResponse detail) throws Exception {
        Font label = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED);
        Font value = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_DARK);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.8f});

        addRow(table, "Comunidad", safe(detail.buildingName()), label, value);
        addRow(table, "Dirección", safe(detail.buildingAddress()), label, value);
        addRow(table, "Comuna", safe(detail.buildingCommune()), label, value);
        addRow(table, "Ciudad", safe(detail.buildingCity()), label, value);
        addRow(table, "Unidad", safe(detail.unitLabel()), label, value);
        addRow(table, "Vencimiento", formatDate(detail.dueDate()), label, value);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addChargesTable(Document document, CommonExpensePeriodDetailResponse detail) throws Exception {
        Font header = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font body = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.6f, 3.2f, 1.2f});

        addHeaderCell(table, "Tipo", header);
        addHeaderCell(table, "Origen", header);
        addHeaderCell(table, "Descripción", header);
        addHeaderCell(table, "Monto", header);

        for (CommonChargeDetailResponse charge : detail.charges()) {
            table.addCell(makeCell(safe(charge.type()), body));
            table.addCell(makeCell(safe(charge.origin()), body));
            table.addCell(makeCell(safe(charge.description()), body));
            table.addCell(makeCell(formatCurrency(charge.amount()), body, Element.ALIGN_RIGHT));
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addTotals(Document document, CommonExpensePeriodDetailResponse detail) throws Exception {
        Font label = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font value = new Font(Font.HELVETICA, 11, Font.BOLD, TEXT_DARK);

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(45);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setSpacingBefore(6);
        totals.setWidths(new float[]{1.2f, 1f});

        totals.addCell(makeCell("Total unidad", label));
        totals.addCell(makeCell(formatCurrency(detail.unitTotal()), value, Element.ALIGN_RIGHT));
        totals.addCell(makeCell("Pagado", label));
        totals.addCell(makeCell(formatCurrency(detail.unitPaid()), value, Element.ALIGN_RIGHT));
        totals.addCell(makeCell("Pendiente", label));
        totals.addCell(makeCell(formatCurrency(detail.unitPending()), value, Element.ALIGN_RIGHT));

        document.add(totals);
        document.add(new Paragraph(" "));
    }

    private void addRevisions(Document document, CommonExpensePeriodDetailResponse detail) throws Exception {
        if (detail.revisions() == null || detail.revisions().isEmpty()) {
            return;
        }
        Font title = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font body = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED);
        Paragraph heading = new Paragraph("Historial de correcciones", title);
        heading.setSpacingBefore(8);
        heading.setSpacingAfter(4);
        document.add(heading);

        for (var revision : detail.revisions()) {
            String line = "• " + safe(revision.action()) + " · " + safe(revision.note());
            document.add(new Paragraph(line, body));
        }
    }

    private void addFooter(Document document) throws Exception {
        Font footer = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);
        Paragraph text = new Paragraph("Documento generado automáticamente por DOMU.", footer);
        text.setSpacingBefore(12);
        document.add(text);
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(makeCell(label, labelFont));
        table.addCell(makeCell(value, valueFont));
    }

    private void addHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = makeCell(text, font);
        cell.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(cell);
    }

    private PdfPCell makeCell(String text, Font font) {
        return makeCell(text, font, Element.ALIGN_LEFT);
    }

    private PdfPCell makeCell(String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(230, 230, 230));
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String formatCurrency(BigDecimal amount) {
        BigDecimal safe = amount != null ? amount : BigDecimal.ZERO;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "CL"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(safe);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return "—";
        }
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatPeriod(Integer year, Integer month) {
        if (year == null || month == null) {
            return "—";
        }
        return String.format("%02d/%d", month, year);
    }
}
