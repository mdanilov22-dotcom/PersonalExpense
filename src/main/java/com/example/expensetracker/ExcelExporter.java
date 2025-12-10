package com.example.expensetracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Класс выполняет экспорт списка расходов в Excel-файлы (.xlsx).
 * Использует библиотеку Apache POI для создания Excel-документов.
 */
public class ExcelExporter {

    private static final Logger logger = LogManager.getLogger(ExcelExporter.class);

    /**
     * Создаёт строку заголовков в Excel-таблице.
     * Заголовки включают: Date, Category, Amount, Description.
     *
     * @param row строка Excel, в которую будут записаны названия столбцов
     */
    private void createHeader(Row row) {
        row.createCell(0).setCellValue("Date");
        row.createCell(1).setCellValue("Category");
        row.createCell(2).setCellValue("Amount");
        row.createCell(3).setCellValue("Description");
    }

    /**
     * Экспортирует все расходы в указанный Excel-файл.
     *
     * @param filename путь и имя создаваемого Excel-файла
     * @param expenses список всех расходов
     * @throws IOException если произошла ошибка записи файла
     */
    public void exportAllExpenses(String filename, List<Expense> expenses) throws IOException {

        logger.info("Exporting ALL expenses to Excel file: {}", filename);

        if (expenses == null || expenses.isEmpty()) {
            logger.warn("No expenses to export (list is empty)");
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("All expenses");
            createHeader(sheet.createRow(0));

            int r = 1;
            for (Expense e : expenses) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.getDate().toString());
                row.createCell(1).setCellValue(e.getCategory());
                row.createCell(2).setCellValue(e.getAmount());
                row.createCell(3).setCellValue(e.getDescription() == null ? "" : e.getDescription());
            }

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                workbook.write(fos);
                logger.info("Successfully exported {} expenses to {}", expenses.size(), filename);
            }

        } catch (IOException ex) {
            logger.error("Error exporting all expenses to {}: {}", filename, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Экспортирует расходы только одной категории в Excel-файл.
     *
     * @param filename путь и имя создаваемого файла
     * @param expenses список расходов указанной категории
     * @param category название категории, по которой выполняется фильтрация
     * @throws IOException если произошла ошибка записи файла
     */
    public void exportCategory(String filename, List<Expense> expenses, String category) throws IOException {

        logger.info("Exporting expenses for category '{}' to file: {}", category, filename);

        if (expenses == null || expenses.isEmpty()) {
            logger.warn("No expenses to export for category '{}'", category);
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Category " + category);
            createHeader(sheet.createRow(0));

            int r = 1;
            for (Expense e : expenses) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(e.getDate().toString());
                row.createCell(1).setCellValue(e.getCategory());
                row.createCell(2).setCellValue(e.getAmount());
                row.createCell(3).setCellValue(e.getDescription() == null ? "" : e.getDescription());
            }

            try (FileOutputStream fos = new FileOutputStream(filename)) {
                workbook.write(fos);
                logger.info("Successfully exported {} records for category '{}' to {}",
                        expenses.size(), category, filename);
            }

        } catch (IOException ex) {
            logger.error("Error exporting category '{}' to {}: {}", category, filename, ex.getMessage());
            throw ex;
        }
    }
}
