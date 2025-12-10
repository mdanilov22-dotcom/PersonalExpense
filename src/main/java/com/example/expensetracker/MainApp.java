package com.example.expensetracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Главный класс консольного приложения трекера расходов.
 * <p>
 * Предоставляет текстовое меню, позволяющее пользователю:
 * <ul>
 *     <li>Добавлять новые расходы</li>
 *     <li>Просматривать общую статистику</li>
 *     <li>Просматривать статистику по категориям</li>
 *     <li>Экспортировать данные в Excel</li>
 *     <li>Завершить работу с сохранением данных</li>
 * </ul>
 * Класс использует {@link ExpenseManager}, {@link Persistence} и {@link ExcelExporter}
 * для управления логикой, сохранением данных и экспортом.
 */
public class MainApp {

    private static final Logger logger = LogManager.getLogger(MainApp.class);

    private static final Scanner scanner = new Scanner(System.in);
    private static final ExpenseManager manager = new ExpenseManager();
    private static final Persistence persistence = new Persistence();
    private static final ExcelExporter excelExporter = new ExcelExporter();

    /**
     * Точка входа в приложение.
     * <p>
     * Загружает сохранённые расходы и запускает главный цикл меню.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {

        logger.info("Application started");

        try {
            List<Expense> loaded = persistence.load();
            loaded.forEach(manager::addExpenseSilently);
            logger.info("Loaded {} expenses from file", loaded.size());
        } catch (IOException e) {
            logger.error("Error loading expenses: {}", e.getMessage());
            System.out.println("Load error: " + e.getMessage());
        }

        while (true) {
            printMenu();
            int choice = readInt();
            logger.info("Menu option selected: {}", choice);

            switch (choice) {
                case 1 -> addExpense();
                case 2 -> showStatistics();
                case 3 -> showCategoryStatistics();
                case 4 -> exportAllToExcel();
                case 5 -> exportCategoryToExcel();
                case 6 -> exitApp();
                default -> {
                    System.out.println("Invalid option");
                    logger.warn("Invalid menu option entered: {}", choice);
                }
            }
        }
    }

    /**
     * Выводит главное меню в консоль.
     */
    private static void printMenu() {
        System.out.println("\n--- MENU ---");
        System.out.println("1. Add expense");
        System.out.println("2. Show total statistics");
        System.out.println("3. Show statistics by category");
        System.out.println("4. Export all to Excel");
        System.out.println("5. Export category to Excel");
        System.out.println("6. Exit");
        System.out.print("Choose: ");
    }

    /**
     * Добавляет новый расход на основе пользовательского ввода.
     * <p>
     * Проверяет корректность категории, суммы и даты.
     */
    private static void addExpense() {
        logger.info("Adding new expense");

        String category;
        while (true) {
            System.out.print("Category (choose from " + ExpenseManager.CATEGORIES + "): ");
            category = scanner.nextLine().trim();

            if (ExpenseManager.CATEGORIES.contains(category)) {
                break;
            } else {
                System.out.println("Invalid category! Please choose one from the list.");
                logger.warn("Invalid category input: {}", category);
            }
        }

        System.out.print("Amount: ");
        double amount = readDouble();

        LocalDate date = readDate();

        System.out.print("Description: ");
        String desc = scanner.nextLine();

        try {
            manager.addExpense(new Expense(category, amount, date, desc));
            logger.info("Expense added: category={}, amount={}, date={}", category, amount, date);
            System.out.println("Expense added.");
        } catch (Exception e) {
            logger.error("Error adding expense: {}", e.getMessage());
            System.out.println("Error adding expense: " + e.getMessage());
        }
    }

    /**
     * Показывает общую статистику расходов.
     * <p>
     * Включает суммарные расходы и распределение по категориям.
     */
    private static void showStatistics() {
        logger.info("Showing total statistics");

        double total = manager.getTotal();
        System.out.println("Total spent: " + total);

        var totals = manager.getTotalByCategory();
        totals.forEach((cat, sum) -> {
            double pct = manager.getPercentage(cat);
            System.out.printf("%s: %.2f (%.2f%%)%n", cat, sum, pct);
        });
    }

    /**
     * Показывает статистику по выбранной категории.
     * <p>
     * Выводит сумму, процент и список расходов.
     */
    private static void showCategoryStatistics() {
        System.out.print("Category: ");
        String cat = scanner.nextLine().trim();

        logger.info("Showing statistics for category: {}", cat);

        double sum = manager.getTotalByCategory().getOrDefault(cat, 0.0);
        double pct = manager.getPercentage(cat);

        System.out.println("Category: " + cat);
        System.out.println("Spent: " + sum);
        System.out.println("Percent: " + pct + "%");

        List<Expense> list = manager.getByCategory(cat);
        list.forEach(System.out::println);
    }

    /**
     * Экспортирует все расходы в Excel-файл.
     * <p>
     * Имя файла: {@code all_expenses.xlsx}.
     */
    private static void exportAllToExcel() {
        logger.info("Exporting ALL expenses to Excel");
        try {
            excelExporter.exportAllExpenses("all_expenses.xlsx", manager.getAllExpenses());
            System.out.println("Exported to all_expenses.xlsx");
        } catch (IOException e) {
            logger.error("Export error: {}", e.getMessage());
            System.out.println("Export error: " + e.getMessage());
        }
    }

    /**
     * Экспортирует расходы выбранной категории в Excel-файл.
     * <p>
     * Имя файла: {@code category_X.xlsx}.
     */
    private static void exportCategoryToExcel() {
        System.out.print("Category: ");
        String cat = scanner.nextLine().trim();

        logger.info("Exporting category '{}' to Excel", cat);

        try {
            excelExporter.exportCategory(
                    "category_" + cat + ".xlsx",
                    manager.getByCategory(cat),
                    cat
            );
            System.out.println("Exported to category_" + cat + ".xlsx");
        } catch (IOException e) {
            logger.error("Export error for category {}: {}", cat, e.getMessage());
            System.out.println("Export error: " + e.getMessage());
        }
    }

    /**
     * Сохраняет все данные и завершает работу приложения.
     * <p>
     * Перед выходом вызывает {@link Persistence#save(List)}.
     */
    private static void exitApp() {
        logger.info("Exiting application, saving expenses...");

        try {
            persistence.save(manager.getAllExpenses());
            System.out.println("Saved.");
            logger.info("Expenses saved successfully");
        } catch (IOException e) {
            logger.error("Save error: {}", e.getMessage());
            System.out.println("Save error: " + e.getMessage());
        }

        System.out.println("Bye!");
        logger.info("Application closed");
        System.exit(0);
    }

    /**
     * Читает целое число из пользовательского ввода.
     * <p>
     * Повторяет запрос до получения корректного числа.
     *
     * @return введённое целое число
     */
    private static int readInt() {
        while (true) {
            try {
                String s = scanner.nextLine();
                return Integer.parseInt(s);
            } catch (Exception e) {
                logger.warn("Invalid integer input");
                System.out.print("Enter number: ");
            }
        }
    }

    /**
     * Читает положительное число с плавающей точкой.
     * <p>
     * Продолжает запрашивать ввод до получения корректного значения.
     *
     * @return положительное значение double
     */
    private static double readDouble() {
        while (true) {
            try {
                String s = scanner.nextLine().trim();
                double value = Double.parseDouble(s);

                if (value <= 0) {
                    logger.warn("Non-positive amount entered: {}", value);
                    System.out.print("Amount must be positive. Try again: ");
                    continue;
                }

                return value;
            } catch (Exception e) {
                logger.warn("Invalid double input");
                System.out.print("Enter a valid number: ");
            }
        }
    }

    /**
     * Читает дату в формате YYYY-MM-DD.
     * <p>
     * Если строка пустая — возвращается текущая дата.
     *
     * @return дата {@link LocalDate}
     */
    private static LocalDate readDate() {
        while (true) {
            System.out.print("Date (yyyy-mm-dd), empty = today: ");
            String d = scanner.nextLine().trim();

            if (d.isEmpty()) {
                return LocalDate.now();
            }

            try {
                return LocalDate.parse(d);
            } catch (Exception e) {
                logger.warn("Invalid date input: {}", d);
                System.out.println("Invalid date! Use format yyyy-mm-dd.");
            }
        }
    }
}
