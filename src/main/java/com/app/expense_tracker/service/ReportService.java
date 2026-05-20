package com.app.expense_tracker.service;

import com.app.expense_tracker.dto.YearlyReportDTO;
import com.app.expense_tracker.entity.Expense;
import com.app.expense_tracker.entity.User;
import com.app.expense_tracker.repository.ExpenseRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.List;

@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;

    public ReportService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    // =========================================================
    // PDF REPORT
    // =========================================================

    public byte[] generatePdf(User user) {

        try {

            List<Expense> expenses =
                    expenseRepository.findByUser(user);

            Document document = new Document();

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            PdfWriter.getInstance(document, out);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    18
            );

            Paragraph title =
                    new Paragraph(
                            "Expense Report",
                            titleFont
                    );

            title.setAlignment(Element.ALIGN_CENTER);

            document.add(title);

            document.add(new Paragraph(" "));
            document.add(
                    new Paragraph(
                            "User : " + user.getUsername()
                    )
            );

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);

            table.addCell("Title");
            table.addCell("Amount");
            table.addCell("Category");
            table.addCell("Date");

            for (Expense expense : expenses) {

                table.addCell(expense.getTitle());

                table.addCell(
                        String.valueOf(
                                expense.getAmount()
                        )
                );

                table.addCell(
                        expense.getCategory()
                );

                table.addCell(
                        expense.getDate().toString()
                );
            }

            document.add(table);

            double total =
                    expenses.stream()
                            .mapToDouble(
                                    Expense::getAmount
                            )
                            .sum();

            document.add(new Paragraph(" "));

            document.add(
                    new Paragraph(
                            "Total Expense : " + total
                    )
            );

            document.close();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "PDF generation failed"
            );
        }
    }

    // =========================================================
    // EXCEL REPORT
    // =========================================================

    public byte[] generateExcel(User user) {

        List<Expense> expenses =
                expenseRepository.findByUser(user);

        try (
                XSSFWorkbook workbook =
                        new XSSFWorkbook();

                ByteArrayOutputStream out =
                        new ByteArrayOutputStream()
        ) {

            XSSFSheet sheet =
                    workbook.createSheet("Expenses");

            int rowNum = 0;

            Row header =
                    sheet.createRow(rowNum++);

            header.createCell(0)
                    .setCellValue("Title");

            header.createCell(1)
                    .setCellValue("Amount");

            header.createCell(2)
                    .setCellValue("Category");

            header.createCell(3)
                    .setCellValue("Date");

            for (Expense expense : expenses) {

                Row row =
                        sheet.createRow(rowNum++);

                row.createCell(0)
                        .setCellValue(
                                expense.getTitle()
                        );

                row.createCell(1)
                        .setCellValue(
                                expense.getAmount()
                        );

                row.createCell(2)
                        .setCellValue(
                                expense.getCategory()
                        );

                row.createCell(3)
                        .setCellValue(
                                expense.getDate()
                                        .toString()
                        );
            }

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Excel generation failed"
            );
        }
    }

    // =========================================================
    // CSV REPORT
    // =========================================================

    public byte[] generateCsv(User user) {

        List<Expense> expenses =
                expenseRepository.findByUser(user);

        try (

                ByteArrayOutputStream out =
                        new ByteArrayOutputStream();

                CSVWriter writer =
                        new CSVWriter(
                                new OutputStreamWriter(out)
                        )

        ) {

            writer.writeNext(new String[]{
                    "Title",
                    "Amount",
                    "Category",
                    "Date"
            });

            for (Expense expense : expenses) {

                writer.writeNext(new String[]{

                        expense.getTitle(),

                        String.valueOf(
                                expense.getAmount()
                        ),

                        expense.getCategory(),

                        expense.getDate().toString()
                });
            }

            writer.flush();

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException(
                    "CSV generation failed"
            );
        }
    }

    // =========================================================
    // MONTHLY REPORT
    // =========================================================

    public List<Expense> monthlyReport(
            User user,
            int year,
            int month
    ) {

        LocalDate start =
                LocalDate.of(year, month, 1);

        LocalDate end =
                start.withDayOfMonth(
                        start.lengthOfMonth()
                );

        return expenseRepository
                .findByUserAndDateBetween(
                        user,
                        start,
                        end
                );
    }

    // =========================================================
    // YEARLY REPORT
    // =========================================================

    public YearlyReportDTO getYearlyReport(
            User user,
            int year
    ) {

        LocalDate startDate =
                LocalDate.of(year, 1, 1);

        LocalDate endDate =
                LocalDate.of(year, 12, 31);

        List<Expense> expenses =
                expenseRepository
                        .findByUserAndDateBetween(
                                user,
                                startDate,
                                endDate
                        );

        double totalExpense = 0;

        Map<String, Double> monthlySummary =
                new LinkedHashMap<>();

        Map<String, Double> categorySummary =
                new LinkedHashMap<>();

        for (Expense expense : expenses) {

            totalExpense += expense.getAmount();

            Month month =
                    expense.getDate().getMonth();

            monthlySummary.put(

                    month.name(),

                    monthlySummary.getOrDefault(
                            month.name(),
                            0.0
                    ) + expense.getAmount()
            );

            categorySummary.put(

                    expense.getCategory(),

                    categorySummary.getOrDefault(
                            expense.getCategory(),
                            0.0
                    ) + expense.getAmount()
            );
        }

        return YearlyReportDTO.builder()
                .year(year)
                .totalExpense(totalExpense)
                .monthlySummary(monthlySummary)
                .categorySummary(categorySummary)
                .build();
    }

    // =========================================================
    // HIGHEST SPENDING MONTH
    // =========================================================

    public String getHighestSpendingMonth(
            Map<String, Double> monthlySummary
    ) {

        String highestMonth = null;

        double max = 0;

        for (Map.Entry<String, Double> entry :
                monthlySummary.entrySet()) {

            if (entry.getValue() > max) {

                max = entry.getValue();

                highestMonth = entry.getKey();
            }
        }

        return highestMonth;
    }

    // =========================================================
    // SAVINGS ANALYSIS
    // =========================================================

    public double calculateSavings(
            double income,
            double expense
    ) {

        return income - expense;
    }

    // =========================================================
    // GROWTH CALCULATION
    // =========================================================

    public double calculateGrowth(
            double current,
            double previous
    ) {

        if (previous == 0) {
            return 0;
        }

        return (
                (current - previous)
                        / previous
        ) * 100;
    }

    // =========================================================
    // YEAR OVER YEAR GROWTH
    // =========================================================

    public double getYearOverYearGrowth(
            User user,
            int currentYear,
            int previousYear
    ) {

        double current =
                getYearlyReport(user, currentYear)
                        .getTotalExpense();

        double previous =
                getYearlyReport(user, previousYear)
                        .getTotalExpense();

        return calculateGrowth(current, previous);
    }

    // =========================================================
    // MOST USED CATEGORY
    // =========================================================

    public String getMostUsedCategory(
            List<Expense> expenses
    ) {

        Map<String, Integer> categoryMap =
                new HashMap<>();

        for (Expense expense : expenses) {

            categoryMap.put(

                    expense.getCategory(),

                    categoryMap.getOrDefault(
                            expense.getCategory(),
                            0
                    ) + 1
            );
        }

        String category = null;

        int max = 0;

        for (Map.Entry<String, Integer> entry :
                categoryMap.entrySet()) {

            if (entry.getValue() > max) {

                max = entry.getValue();

                category = entry.getKey();
            }
        }

        return category;
    }

    // =========================================================
    // MONTHLY COMPARISON
    // =========================================================

    public double compareMonths(
            User user,
            int year,
            int month1,
            int month2
    ) {

        double first =
                monthlyReport(user, year, month1)
                        .stream()
                        .mapToDouble(
                                Expense::getAmount
                        )
                        .sum();

        double second =
                monthlyReport(user, year, month2)
                        .stream()
                        .mapToDouble(
                                Expense::getAmount
                        )
                        .sum();

        return calculateGrowth(second, first);
    }

    // =========================================================
    // AVERAGE MONTHLY EXPENSE
    // =========================================================

    public double getAverageMonthlyExpense(
            User user,
            int year
    ) {

        YearlyReportDTO report =
                getYearlyReport(user, year);

        return report.getTotalExpense() / 12;
    }

    // =========================================================
    // HIGHEST EXPENSE
    // =========================================================

    public Expense getHighestExpense(User user) {

        List<Expense> expenses =
                expenseRepository.findByUser(user);

        return expenses.stream()
                .max(
                        Comparator.comparingDouble(
                                Expense::getAmount
                        )
                )
                .orElse(null);
    }

    // =========================================================
    // LOWEST EXPENSE
    // =========================================================

    public Expense getLowestExpense(User user) {

        List<Expense> expenses =
                expenseRepository.findByUser(user);

        return expenses.stream()
                .min(
                        Comparator.comparingDouble(
                                Expense::getAmount
                        )
                )
                .orElse(null);
    }

    // =========================================================
    // MONTHLY TREND
    // =========================================================

    public Map<String, Double> monthlyTrend(
            User user,
            int year
    ) {

        return getYearlyReport(user, year)
                .getMonthlySummary();
    }

    // =========================================================
    // CATEGORY PERCENTAGE
    // =========================================================

    public Map<String, Double> categoryPercentage(
            User user,
            int year
    ) {

        YearlyReportDTO report =
                getYearlyReport(user, year);

        double total =
                report.getTotalExpense();

        Map<String, Double> result =
                new LinkedHashMap<>();

        for (Map.Entry<String, Double> entry :
                report.getCategorySummary()
                        .entrySet()) {

            result.put(

                    entry.getKey(),

                    (entry.getValue() / total) * 100
            );
        }

        return result;
    }

    // =========================================================
    // REMAINING BUDGET
    // =========================================================

    public double remainingBudget(
            double budget,
            double spent
    ) {

        return budget - spent;
    }
}