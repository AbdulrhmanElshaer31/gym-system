package com.gym.service;

import com.gym.entity.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.io.font.constants.StandardFonts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final FinancialService financialService;
    private final ProductService productService;
    private final MemberService memberService;

    // Brand colors
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(29, 84, 109); // #1D546D
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(95, 149, 152); // #5F9598
    private static final DeviceRgb DARK_COLOR = new DeviceRgb(6, 30, 41); // #061E29
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(243, 244, 244); // #F3F4F4
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(34, 197, 94);
    private static final DeviceRgb DANGER_COLOR = new DeviceRgb(239, 68, 68);
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(245, 158, 11);
    private static final DeviceRgb GRAY_COLOR = new DeviceRgb(100, 116, 139);

    /**
     * تقرير مالي مفصل احترافي
     */
    public File exportDetailedFinancialReport(LocalDate startDate, LocalDate endDate, String gymName) throws Exception {
        FinancialService.FinancialSummary summary = financialService.getFinancialSummary(startDate, endDate);
        return exportDetailedFinancialReportFiltered(startDate, endDate, summary.transactions(), "الكل", gymName);
    }

    /**
     * تقرير مالي مفصل احترافي - مع فلتر
     */
    public File exportDetailedFinancialReportFiltered(LocalDate startDate, LocalDate endDate, 
                                                       List<Transaction> filteredTransactions,
                                                       String filterName, String gymName) throws Exception {
        // Calculate summary from filtered transactions
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<Transaction.TransactionCategory, BigDecimal> incomeBreakdown = new java.util.HashMap<>();
        Map<Transaction.TransactionCategory, BigDecimal> expenseBreakdown = new java.util.HashMap<>();
        
        for (Transaction t : filteredTransactions) {
            if (t.getType() == Transaction.TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
                incomeBreakdown.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            } else {
                totalExpenses = totalExpenses.add(t.getAmount());
                expenseBreakdown.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            }
        }
        
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);
        FinancialService.FinancialSummary summary = new FinancialService.FinancialSummary(
            totalIncome, totalExpenses, netProfit, incomeBreakdown, expenseBreakdown, filteredTransactions, startDate, endDate
        );
        
        String filterSuffix = filterName != null && !filterName.equals("الكل") ? "_" + filterName : "";
        String fileName = "detailed_financial_report" + filterSuffix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(30, 30, 30, 30);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // === Page 1: Cover & Executive Summary ===
        addReportCover(document, gymName, startDate, endDate, fontBold, font);
        
        // Add filter note if filtered
        if (filterName != null && !filterName.equals("الكل")) {
            Paragraph filterNote = new Paragraph("🔍 الفلتر المطبق: " + filterName)
                    .setFont(fontBold)
                    .setFontSize(12)
                    .setFontColor(ACCENT_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10);
            document.add(filterNote);
        }
        
        addExecutiveSummary(document, summary, startDate, endDate, font, fontBold);
        
        // === Page 2: Detailed Analysis ===
        document.add(new AreaBreak());
        addSectionHeader(document, "التحليل المالي التفصيلي", fontBold);
        addDetailedIncomeAnalysis(document, summary, font, fontBold);
        addDetailedExpenseAnalysis(document, summary, font, fontBold);
        
        // === Page 3: Transaction Log ===
        document.add(new AreaBreak());
        addSectionHeader(document, "سجل المعاملات التفصيلي", fontBold);
        addTransactionLog(document, filteredTransactions, font, fontBold);
        
        // === Page 4: Daily Breakdown ===
        document.add(new AreaBreak());
        addSectionHeader(document, "التحليل اليومي", fontBold);
        addDailyBreakdown(document, filteredTransactions, startDate, endDate, font, fontBold);
        
        // === Footer on all pages ===
        addProfessionalFooter(document, font, gymName);

        document.close();
        return file;
    }

    /**
     * تقرير مالي مختصر
     */
    public File exportFinancialReport(LocalDate startDate, LocalDate endDate, String gymName) throws Exception {
        FinancialService.FinancialSummary summary = financialService.getFinancialSummary(startDate, endDate);
        return exportFinancialReportFiltered(startDate, endDate, summary.transactions(), "الكل", gymName);
    }

    /**
     * تقرير مالي مختصر - مع فلتر
     */
    public File exportFinancialReportFiltered(LocalDate startDate, LocalDate endDate, 
                                               List<Transaction> filteredTransactions,
                                               String filterName, String gymName) throws Exception {
        // Calculate summary from filtered transactions
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        Map<Transaction.TransactionCategory, BigDecimal> incomeBreakdown = new java.util.HashMap<>();
        Map<Transaction.TransactionCategory, BigDecimal> expenseBreakdown = new java.util.HashMap<>();
        
        for (Transaction t : filteredTransactions) {
            if (t.getType() == Transaction.TransactionType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
                incomeBreakdown.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            } else {
                totalExpenses = totalExpenses.add(t.getAmount());
                expenseBreakdown.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
            }
        }
        
        BigDecimal netProfit = totalIncome.subtract(totalExpenses);
        FinancialService.FinancialSummary summary = new FinancialService.FinancialSummary(
            totalIncome, totalExpenses, netProfit, incomeBreakdown, expenseBreakdown, filteredTransactions, startDate, endDate
        );
        
        String filterSuffix = filterName != null && !filterName.equals("الكل") ? "_" + filterName : "";
        String fileName = "financial_report" + filterSuffix + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Header
        addHeader(document, gymName, fontBold);
        
        // Report Title
        Paragraph title = new Paragraph("Financial Summary Report")
                .setFont(fontBold)
                .setFontSize(22)
                .setFontColor(DARK_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        // Date Range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Paragraph dateRange = new Paragraph("Period: " + startDate.format(formatter) + " - " + endDate.format(formatter))
                .setFont(font)
                .setFontSize(12)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(dateRange);

        // Add filter note if filtered
        if (filterName != null && !filterName.equals("الكل")) {
            Paragraph filterNote = new Paragraph("Filter: " + filterName)
                    .setFont(fontBold)
                    .setFontSize(11)
                    .setFontColor(ACCENT_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(filterNote);
        } else {
            document.add(new Paragraph("\n").setMarginBottom(15));
        }

        // Summary Cards
        addSummarySection(document, summary, font, fontBold);

        // Income Breakdown
        document.add(new Paragraph("\n"));
        addCategoryTable(document, "Income Breakdown", summary.incomeBreakdown(), font, fontBold, SUCCESS_COLOR);

        // Expense Breakdown
        document.add(new Paragraph("\n"));
        addCategoryTable(document, "Expense Breakdown", summary.expenseBreakdown(), font, fontBold, DANGER_COLOR);

        // Transactions Table
        document.add(new Paragraph("\n"));
        addTransactionsTable(document, filteredTransactions, font, fontBold);

        // Footer
        addFooter(document, font);

        document.close();
        return file;
    }

    private void addReportCover(Document document, String gymName, LocalDate startDate, LocalDate endDate, 
                                 PdfFont fontBold, PdfFont font) {
        // Gym name
        Paragraph gymTitle = new Paragraph(gymName != null && !gymName.isEmpty() ? gymName : "GYM MANAGEMENT SYSTEM")
                .setFont(fontBold)
                .setFontSize(32)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(50)
                .setMarginBottom(30);
        document.add(gymTitle);

        // Decorative line
        Table divider = new Table(1).setWidth(UnitValue.createPercentValue(60)).setHorizontalAlignment(HorizontalAlignment.CENTER);
        Cell dividerCell = new Cell().setBackgroundColor(ACCENT_COLOR).setHeight(4).setBorder(Border.NO_BORDER);
        divider.addCell(dividerCell);
        document.add(divider);

        // Report title
        Paragraph reportTitle = new Paragraph("التقرير المالي الشامل")
                .setFont(fontBold)
                .setFontSize(28)
                .setFontColor(DARK_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40)
                .setMarginBottom(10);
        document.add(reportTitle);

        Paragraph reportSubtitle = new Paragraph("Comprehensive Financial Report")
                .setFont(font)
                .setFontSize(16)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(50);
        document.add(reportSubtitle);

        // Period info box
        Table periodBox = new Table(1).setWidth(UnitValue.createPercentValue(70)).setHorizontalAlignment(HorizontalAlignment.CENTER);
        Cell periodCell = new Cell()
                .setBackgroundColor(LIGHT_BG)
                .setBorder(new SolidBorder(ACCENT_COLOR, 2))
                .setPadding(20);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        Paragraph periodTitle = new Paragraph("فترة التقرير")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER);
        
        Paragraph periodDates = new Paragraph(startDate.format(formatter) + "  ←  " + endDate.format(formatter))
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(DARK_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);

        Paragraph periodDays = new Paragraph("(" + days + " يوم)")
                .setFont(font)
                .setFontSize(12)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(5);

        periodCell.add(periodTitle);
        periodCell.add(periodDates);
        periodCell.add(periodDays);
        periodBox.addCell(periodCell);
        document.add(periodBox);

        // Generation info
        Paragraph genInfo = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
                .setFont(font)
                .setFontSize(10)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(80);
        document.add(genInfo);
    }

    private void addExecutiveSummary(Document document, FinancialService.FinancialSummary summary, 
                                      LocalDate startDate, LocalDate endDate, PdfFont font, PdfFont fontBold) {
        document.add(new Paragraph("\n\n"));
        
        addSectionHeader(document, "الملخص التنفيذي / Executive Summary", fontBold);

        // Main metrics in a 3-column layout
        Table metricsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(25);

        // Total Income
        addMetricCard(metricsTable, "إجمالي الإيرادات", "Total Income", 
                     formatCurrency(summary.totalIncome()), fontBold, font, SUCCESS_COLOR);
        
        // Total Expenses
        addMetricCard(metricsTable, "إجمالي المصروفات", "Total Expenses", 
                     formatCurrency(summary.totalExpenses()), fontBold, font, DANGER_COLOR);
        
        // Net Profit
        DeviceRgb profitColor = summary.netProfit().compareTo(BigDecimal.ZERO) >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        addMetricCard(metricsTable, "صافي الربح", "Net Profit", 
                     formatCurrency(summary.netProfit()), fontBold, font, profitColor);

        document.add(metricsTable);

        // Additional stats
        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        BigDecimal dailyAvg = days > 0 ? summary.netProfit().divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (summary.totalIncome().compareTo(BigDecimal.ZERO) > 0) {
            profitMargin = summary.netProfit()
                .multiply(BigDecimal.valueOf(100))
                .divide(summary.totalIncome(), 1, RoundingMode.HALF_UP);
        }

        addSmallStat(statsTable, "عدد المعاملات", String.valueOf(summary.transactions().size()), font, fontBold);
        addSmallStat(statsTable, "متوسط يومي", formatCurrency(dailyAvg), font, fontBold);
        addSmallStat(statsTable, "نسبة الربح", profitMargin + "%", font, fontBold);
        addSmallStat(statsTable, "عدد الأيام", days + " يوم", font, fontBold);

        document.add(statsTable);
    }

    private void addMetricCard(Table table, String arabicLabel, String englishLabel, 
                                String value, PdfFont fontBold, PdfFont font, DeviceRgb color) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15)
                .setBackgroundColor(LIGHT_BG);

        cell.add(new Paragraph(arabicLabel)
                .setFont(fontBold)
                .setFontSize(12)
                .setFontColor(DARK_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
        
        cell.add(new Paragraph(englishLabel)
                .setFont(font)
                .setFontSize(9)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
        
        cell.add(new Paragraph(value)
                .setFont(fontBold)
                .setFontSize(22)
                .setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));

        table.addCell(cell);
    }

    private void addSmallStat(Table table, String label, String value, PdfFont font, PdfFont fontBold) {
        Cell cell = new Cell()
                .setBorder(new SolidBorder(LIGHT_BG, 1))
                .setPadding(10);

        cell.add(new Paragraph(label)
                .setFont(font)
                .setFontSize(9)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
        
        cell.add(new Paragraph(value)
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));

        table.addCell(cell);
    }

    private void addSectionHeader(Document document, String title, PdfFont fontBold) {
        Table headerTable = new Table(1).setWidth(UnitValue.createPercentValue(100));
        Cell headerCell = new Cell()
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(12)
                .setBorder(Border.NO_BORDER);
        
        headerCell.add(new Paragraph(title)
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER));
        
        headerTable.addCell(headerCell);
        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    private void addDetailedIncomeAnalysis(Document document, FinancialService.FinancialSummary summary, 
                                            PdfFont font, PdfFont fontBold) {
        Paragraph title = new Paragraph("تحليل الإيرادات / Income Analysis")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("التصنيف / Category", fontBold, SUCCESS_COLOR));
        table.addHeaderCell(createHeaderCell("المبلغ / Amount", fontBold, SUCCESS_COLOR));
        table.addHeaderCell(createHeaderCell("النسبة / %", fontBold, SUCCESS_COLOR));

        BigDecimal total = summary.totalIncome();
        for (Map.Entry<Transaction.TransactionCategory, BigDecimal> entry : summary.incomeBreakdown().entrySet()) {
            table.addCell(createDataCell(entry.getKey().getArabicName(), font));
            table.addCell(createDataCell(formatCurrency(entry.getValue()), font));
            
            String percent = total.compareTo(BigDecimal.ZERO) > 0 
                ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP) + "%"
                : "0%";
            table.addCell(createDataCell(percent, font));
        }

        // Total row
        table.addCell(createDataCell("الإجمالي / Total", fontBold).setBackgroundColor(new DeviceRgb(220, 252, 231)));
        table.addCell(createDataCell(formatCurrency(total), fontBold).setBackgroundColor(new DeviceRgb(220, 252, 231)));
        table.addCell(createDataCell("100%", fontBold).setBackgroundColor(new DeviceRgb(220, 252, 231)));

        document.add(table);
    }

    private void addDetailedExpenseAnalysis(Document document, FinancialService.FinancialSummary summary, 
                                             PdfFont font, PdfFont fontBold) {
        Paragraph title = new Paragraph("تحليل المصروفات / Expense Analysis")
                .setFont(fontBold)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10);
        document.add(title);

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Header
        table.addHeaderCell(createHeaderCell("التصنيف / Category", fontBold, DANGER_COLOR));
        table.addHeaderCell(createHeaderCell("المبلغ / Amount", fontBold, DANGER_COLOR));
        table.addHeaderCell(createHeaderCell("النسبة / %", fontBold, DANGER_COLOR));

        BigDecimal total = summary.totalExpenses();
        for (Map.Entry<Transaction.TransactionCategory, BigDecimal> entry : summary.expenseBreakdown().entrySet()) {
            table.addCell(createDataCell(entry.getKey().getArabicName(), font));
            table.addCell(createDataCell(formatCurrency(entry.getValue()), font));
            
            String percent = total.compareTo(BigDecimal.ZERO) > 0 
                ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP) + "%"
                : "0%";
            table.addCell(createDataCell(percent, font));
        }

        // Total row
        table.addCell(createDataCell("الإجمالي / Total", fontBold).setBackgroundColor(new DeviceRgb(254, 226, 226)));
        table.addCell(createDataCell(formatCurrency(total), fontBold).setBackgroundColor(new DeviceRgb(254, 226, 226)));
        table.addCell(createDataCell("100%", fontBold).setBackgroundColor(new DeviceRgb(254, 226, 226)));

        document.add(table);
    }

    private void addTransactionLog(Document document, List<Transaction> transactions, PdfFont font, PdfFont fontBold) {
        Paragraph info = new Paragraph("إجمالي عدد المعاملات: " + transactions.size())
                .setFont(font)
                .setFontSize(11)
                .setFontColor(GRAY_COLOR)
                .setMarginBottom(15);
        document.add(info);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1.5f, 2.5f, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("التاريخ", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("النوع", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("التصنيف", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("الوصف", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("المبلغ", fontBold, PRIMARY_COLOR));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int count = 0;
        
        for (Transaction t : transactions) {
            if (count >= 50) break; // Limit to 50 transactions per page
            
            table.addCell(createDataCell(t.getTransactionDate().format(formatter), font));
            
            Cell typeCell = createDataCell(t.getType().getArabicName(), font);
            typeCell.setFontColor(t.getType() == Transaction.TransactionType.INCOME ? SUCCESS_COLOR : DANGER_COLOR);
            table.addCell(typeCell);
            
            table.addCell(createDataCell(t.getCategory().getArabicName(), font));
            table.addCell(createDataCell(t.getDescription(), font).setTextAlignment(TextAlignment.LEFT));
            
            Cell amountCell = createDataCell(formatCurrency(t.getAmount()), font);
            amountCell.setFontColor(t.getType() == Transaction.TransactionType.INCOME ? SUCCESS_COLOR : DANGER_COLOR);
            table.addCell(amountCell);
            
            count++;
        }

        document.add(table);

        if (transactions.size() > 50) {
            Paragraph more = new Paragraph("... و " + (transactions.size() - 50) + " معاملة أخرى")
                    .setFont(font)
                    .setFontSize(10)
                    .setFontColor(GRAY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10);
            document.add(more);
        }
    }

    private void addDailyBreakdown(Document document, List<Transaction> transactions, 
                                    LocalDate startDate, LocalDate endDate, PdfFont font, PdfFont fontBold) {
        // Group transactions by date
        Map<LocalDate, List<Transaction>> byDate = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getTransactionDate));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1, 1, 1, 0.5f}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("التاريخ", fontBold, ACCENT_COLOR));
        table.addHeaderCell(createHeaderCell("الإيرادات", fontBold, ACCENT_COLOR));
        table.addHeaderCell(createHeaderCell("المصروفات", fontBold, ACCENT_COLOR));
        table.addHeaderCell(createHeaderCell("الصافي", fontBold, ACCENT_COLOR));
        table.addHeaderCell(createHeaderCell("#", fontBold, ACCENT_COLOR));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM (EEE)");
        LocalDate current = startDate;
        int displayedDays = 0;
        
        while (!current.isAfter(endDate) && displayedDays < 31) {
            List<Transaction> dayTransactions = byDate.getOrDefault(current, Collections.emptyList());
            
            BigDecimal dayIncome = dayTransactions.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.INCOME)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dayExpense = dayTransactions.stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal dayNet = dayIncome.subtract(dayExpense);

            table.addCell(createDataCell(current.format(formatter), font));
            
            Cell incomeCell = createDataCell(formatCurrency(dayIncome), font);
            if (dayIncome.compareTo(BigDecimal.ZERO) > 0) {
                incomeCell.setFontColor(SUCCESS_COLOR);
            }
            table.addCell(incomeCell);
            
            Cell expenseCell = createDataCell(formatCurrency(dayExpense), font);
            if (dayExpense.compareTo(BigDecimal.ZERO) > 0) {
                expenseCell.setFontColor(DANGER_COLOR);
            }
            table.addCell(expenseCell);
            
            Cell netCell = createDataCell(formatCurrency(dayNet), fontBold);
            netCell.setFontColor(dayNet.compareTo(BigDecimal.ZERO) >= 0 ? SUCCESS_COLOR : DANGER_COLOR);
            table.addCell(netCell);
            
            table.addCell(createDataCell(String.valueOf(dayTransactions.size()), font));
            
            current = current.plusDays(1);
            displayedDays++;
        }

        document.add(table);
    }

    private void addProfessionalFooter(Document document, PdfFont font, String gymName) {
        document.add(new Paragraph("\n"));
        
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph(gymName != null ? gymName : "GYM Management System")
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(GRAY_COLOR));
        footer.addCell(leftCell);

        Cell centerCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph("تم إنشاء التقرير: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(GRAY_COLOR));
        footer.addCell(centerCell);

        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("توقيع المدير: _______________")
                        .setFont(font)
                        .setFontSize(9)
                        .setFontColor(GRAY_COLOR));
        footer.addCell(rightCell);

        document.add(footer);
    }

    // ================== INVENTORY REPORT ==================

    public File exportInventoryReport(LocalDate startDate, LocalDate endDate, String gymName) throws Exception {
        List<Product> products = productService.getAllActiveProducts();
        List<InventoryLog> logs = productService.getInventoryLogsByDateRange(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
        
        String fileName = "inventory_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);
        
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Header
        addHeader(document, gymName, fontBold);
        
        // Report Title
        Paragraph title = new Paragraph("Inventory & Stock Report")
                .setFont(fontBold)
                .setFontSize(24)
                .setFontColor(DARK_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(title);

        // Date Range
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Paragraph dateRange = new Paragraph("Period: " + startDate.format(formatter) + " - " + endDate.format(formatter))
                .setFont(font)
                .setFontSize(12)
                .setFontColor(GRAY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(dateRange);

        // Inventory Summary
        BigDecimal totalValue = productService.calculateTotalInventoryValue();
        BigDecimal potentialRevenue = productService.calculatePotentialRevenue();
        
        Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);
        
        addSummaryCard(summaryTable, "Total Inventory Value", formatCurrency(totalValue), font, fontBold, PRIMARY_COLOR);
        addSummaryCard(summaryTable, "Potential Revenue", formatCurrency(potentialRevenue), font, fontBold, SUCCESS_COLOR);
        document.add(summaryTable);

        // Current Stock Table
        addStockTable(document, products, font, fontBold);

        // Inventory Movements
        if (!logs.isEmpty()) {
            document.add(new Paragraph("\n"));
            addInventoryLogsTable(document, logs, font, fontBold);
        }

        // Footer
        addFooter(document, font);

        document.close();
        return file;
    }

    // ================== HELPER METHODS ==================

    private void addHeader(Document document, String gymName, PdfFont fontBold) {
        Paragraph header = new Paragraph(gymName != null && !gymName.isEmpty() ? gymName : "GYM MANAGEMENT SYSTEM")
                .setFont(fontBold)
                .setFontSize(28)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(header);
        
        // Divider line
        Table divider = new Table(1).setWidth(UnitValue.createPercentValue(100));
        Cell cell = new Cell().setBackgroundColor(PRIMARY_COLOR).setHeight(3).setBorder(Border.NO_BORDER);
        divider.addCell(cell);
        document.add(divider);
        document.add(new Paragraph("\n"));
    }

    private void addSummarySection(Document document, FinancialService.FinancialSummary summary, 
                                    PdfFont font, PdfFont fontBold) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        addSummaryCard(table, "Total Income", formatCurrency(summary.totalIncome()), font, fontBold, SUCCESS_COLOR);
        addSummaryCard(table, "Total Expenses", formatCurrency(summary.totalExpenses()), font, fontBold, DANGER_COLOR);
        
        DeviceRgb profitColor = summary.netProfit().compareTo(BigDecimal.ZERO) >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        addSummaryCard(table, "Net Profit", formatCurrency(summary.netProfit()), font, fontBold, profitColor);

        document.add(table);
    }

    private void addSummaryCard(Table table, String label, String value, PdfFont font, PdfFont fontBold, DeviceRgb color) {
        Cell cell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(15)
                .setBackgroundColor(LIGHT_BG);

        Paragraph labelPara = new Paragraph(label)
                .setFont(font)
                .setFontSize(11)
                .setFontColor(GRAY_COLOR);
        
        Paragraph valuePara = new Paragraph(value)
                .setFont(fontBold)
                .setFontSize(20)
                .setFontColor(color);

        cell.add(labelPara);
        cell.add(valuePara);
        table.addCell(cell);
    }

    private void addCategoryTable(Document document, String title, 
                                   Map<Transaction.TransactionCategory, BigDecimal> data,
                                   PdfFont font, PdfFont fontBold, DeviceRgb headerColor) {
        Paragraph sectionTitle = new Paragraph(title)
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(DARK_COLOR)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("Category", fontBold, headerColor));
        table.addHeaderCell(createHeaderCell("Amount", fontBold, headerColor));

        // Data rows
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Transaction.TransactionCategory, BigDecimal> entry : data.entrySet()) {
            table.addCell(createDataCell(entry.getKey().getArabicName(), font));
            table.addCell(createDataCell(formatCurrency(entry.getValue()), font));
            total = total.add(entry.getValue());
        }

        // Total row
        table.addCell(createDataCell("Total", fontBold).setBackgroundColor(new DeviceRgb(241, 245, 249)));
        table.addCell(createDataCell(formatCurrency(total), fontBold).setBackgroundColor(new DeviceRgb(241, 245, 249)));

        document.add(table);
    }

    private void addTransactionsTable(Document document, List<Transaction> transactions,
                                       PdfFont font, PdfFont fontBold) {
        Paragraph sectionTitle = new Paragraph("Transaction Details")
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(DARK_COLOR)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 2, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("Date", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Type", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Description", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Amount", fontBold, PRIMARY_COLOR));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (Transaction t : transactions) {
            table.addCell(createDataCell(t.getTransactionDate().format(formatter), font));
            table.addCell(createDataCell(t.getType().getArabicName(), font));
            table.addCell(createDataCell(t.getDescription(), font));
            
            Cell amountCell = createDataCell(formatCurrency(t.getAmount()), font);
            if (t.getType() == Transaction.TransactionType.INCOME) {
                amountCell.setFontColor(SUCCESS_COLOR);
            } else {
                amountCell.setFontColor(DANGER_COLOR);
            }
            table.addCell(amountCell);
        }

        document.add(table);
    }

    private void addStockTable(Document document, List<Product> products, PdfFont font, PdfFont fontBold) {
        Paragraph sectionTitle = new Paragraph("Current Stock")
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(DARK_COLOR)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 1, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("Product", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Category", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Quantity", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Purchase Price", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Sale Price", fontBold, PRIMARY_COLOR));

        for (Product p : products) {
            table.addCell(createDataCell(p.getName(), font));
            table.addCell(createDataCell(p.getCategory().getArabicName(), font));
            
            Cell qtyCell = createDataCell(String.valueOf(p.getQuantity()), font);
            if (p.isLowStock()) {
                qtyCell.setFontColor(DANGER_COLOR);
            }
            table.addCell(qtyCell);
            
            table.addCell(createDataCell(formatCurrency(p.getPurchasePrice()), font));
            table.addCell(createDataCell(formatCurrency(p.getSalePrice()), font));
        }

        document.add(table);
    }

    private void addInventoryLogsTable(Document document, List<InventoryLog> logs, 
                                        PdfFont font, PdfFont fontBold) {
        Paragraph sectionTitle = new Paragraph("Inventory Movements")
                .setFont(fontBold)
                .setFontSize(16)
                .setFontColor(DARK_COLOR)
                .setMarginBottom(10);
        document.add(sectionTitle);

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 1, 1, 2}))
                .setWidth(UnitValue.createPercentValue(100));

        // Header
        table.addHeaderCell(createHeaderCell("Date", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Product", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Type", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Before", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("After", fontBold, PRIMARY_COLOR));
        table.addHeaderCell(createHeaderCell("Reason", fontBold, PRIMARY_COLOR));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (InventoryLog log : logs) {
            table.addCell(createDataCell(log.getCreatedAt().format(formatter), font));
            table.addCell(createDataCell(log.getProduct().getName(), font));
            table.addCell(createDataCell(log.getType().getArabicName(), font));
            table.addCell(createDataCell(String.valueOf(log.getQuantityBefore()), font));
            table.addCell(createDataCell(String.valueOf(log.getQuantityAfter()), font));
            table.addCell(createDataCell(log.getReason() != null ? log.getReason() : "-", font));
        }

        document.add(table);
    }

    private void addFooter(Document document, PdfFont font) {
        document.add(new Paragraph("\n\n"));
        
        Table footer = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell dateCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                        .setFont(font)
                        .setFontSize(10)
                        .setFontColor(GRAY_COLOR));
        footer.addCell(dateCell);

        Cell signatureCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Manager Signature: ___________________")
                        .setFont(font)
                        .setFontSize(10)
                        .setFontColor(GRAY_COLOR));
        footer.addCell(signatureCell);

        document.add(footer);
    }

    private Cell createHeaderCell(String text, PdfFont font, DeviceRgb color) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(11).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(color)
                .setPadding(8)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private Cell createDataCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00 EGP";
        return String.format("%,.2f EGP", amount);
    }
}
