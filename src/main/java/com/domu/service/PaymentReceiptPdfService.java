package com.domu.service;

import com.domu.domain.finance.CommonPayment;
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

public class PaymentReceiptPdfService {

    private static final Color DOMU_YELLOW = new Color(247, 206, 15);
    private static final Color DOMU_TURQUOISE = new Color(83, 164, 151);
    private static final Color TEXT_DARK = new Color(20, 20, 20);
    private static final Color TEXT_MUTED = new Color(90, 90, 90);
    private static final Color SUCCESS_GREEN = new Color(16, 185, 129);

    public byte[] buildPaymentReceipt(CommonPayment payment, String buildingName, String unitLabel, String chargeDescription) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 48, 42);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            document.open();
            addWatermark(writer);
            addBanner(document);
            addTitle(document);
            addPaymentDetails(document, payment, buildingName, unitLabel, chargeDescription);
            addAmountBox(document, payment.amount());
            addFooter(document);
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new ValidationException("No se pudo generar el comprobante de pago: " + e.getMessage());
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
        }
    }

    private void addBanner(Document document) throws Exception {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(DOMU_TURQUOISE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        cell.setMinimumHeight(8);
        banner.addCell(cell);
        document.add(banner);

        PdfPTable accent = new PdfPTable(1);
        accent.setWidthPercentage(100);
        PdfPCell accentCell = new PdfPCell();
        accentCell.setBackgroundColor(DOMU_YELLOW);
        accentCell.setFixedHeight(3);
        accentCell.setBorder(Rectangle.NO_BORDER);
        accent.addCell(accentCell);
        document.add(accent);

        document.add(new Paragraph(" "));
    }

    private void addTitle(Document document) throws Exception {
        Font titleFont = new Font(Font.HELVETICA, 20, Font.BOLD, TEXT_DARK);
        Font subtitleFont = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_MUTED);

        Paragraph title = new Paragraph("Comprobante de Pago", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph subtitle = new Paragraph("DOMU - Administración de Comunidades", subtitleFont);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(20);
        document.add(subtitle);
    }

    private void addPaymentDetails(Document document, CommonPayment payment, String buildingName, String unitLabel, String chargeDescription) throws Exception {
        Font label = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED);
        Font value = new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_DARK);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.8f});

        addRow(table, "N° Comprobante", "#" + payment.id(), label, value);
        addRow(table, "Fecha de Pago", formatDate(payment.issuedAt()), label, value);
        addRow(table, "Comunidad", safe(buildingName), label, value);
        addRow(table, "Unidad", safe(unitLabel), label, value);
        addRow(table, "Concepto", safe(chargeDescription), label, value);
        addRow(table, "Método de Pago", safe(payment.paymentMethod()), label, value);
        addRow(table, "Referencia", safe(payment.reference()), label, value);
        addRow(table, "Estado", "CONFIRMADO", label, value);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addAmountBox(Document document, BigDecimal amount) throws Exception {
        Font amountLabel = new Font(Font.HELVETICA, 12, Font.BOLD, TEXT_MUTED);
        Font amountValue = new Font(Font.HELVETICA, 28, Font.BOLD, SUCCESS_GREEN);

        PdfPTable box = new PdfPTable(1);
        box.setWidthPercentage(60);
        box.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(236, 253, 245));
        cell.setBorderColor(SUCCESS_GREEN);
        cell.setBorderWidth(2);
        cell.setPadding(20);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph labelPara = new Paragraph("MONTO PAGADO", amountLabel);
        labelPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(labelPara);

        Paragraph amountPara = new Paragraph(formatCurrency(amount), amountValue);
        amountPara.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(amountPara);

        box.addCell(cell);
        document.add(box);
        document.add(new Paragraph(" "));
    }

    private void addFooter(Document document) throws Exception {
        Font footer = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

        Paragraph text1 = new Paragraph("Este comprobante es válido como constancia de pago.", footer);
        text1.setAlignment(Element.ALIGN_CENTER);
        text1.setSpacingBefore(30);
        document.add(text1);

        Paragraph text2 = new Paragraph("Documento generado automáticamente por DOMU.", footer);
        text2.setAlignment(Element.ALIGN_CENTER);
        document.add(text2);

        Paragraph text3 = new Paragraph("Fecha de emisión: " + formatDate(LocalDate.now()), footer);
        text3.setAlignment(Element.ALIGN_CENTER);
        document.add(text3);
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(makeCell(label, labelFont));
        table.addCell(makeCell(value, valueFont));
    }

    private PdfPCell makeCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorderColor(new Color(230, 230, 230));
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
}
