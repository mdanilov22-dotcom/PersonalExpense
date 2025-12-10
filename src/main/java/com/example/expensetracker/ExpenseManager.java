package com.example.expensetracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс управляет коллекцией расходов.
 * Предоставляет методы для добавления, фильтрации и анализа данных.
 * Все операции потокобезопасны благодаря synchronized.
 */
public class ExpenseManager {

    private static final Logger logger = LogManager.getLogger(ExpenseManager.class);

    /**
     * Предопределённый список категорий расходов.
     * Используется для валидации и отображения в интерфейсе.
     */
    public static final List<String> CATEGORIES = Arrays.asList(
            "Food", "Transport", "Housing", "Utilities", "Entertainment",
            "Health", "Education", "Clothing", "Gifts", "Miscellaneous"
    );

    /** Внутренний список расходов. */
    private final List<Expense> expenses = new ArrayList<>();

    /**
     * Создаёт новый менеджер расходов.
     * Начинает с пустого списка.
     */
    public ExpenseManager() {
        logger.info("ExpenseManager initialized");
    }

    /**
     * Получает список доступных категорий.
     *
     * @return неизменяемый список строк категорий
     */
    public List<String> getCategories() {
        return Collections.unmodifiableList(CATEGORIES);
    }

    /**
     * Добавляет расход в список.
     * Выполняет проверку на корректность суммы и существование категории.
     *
     * @param e объект расхода
     * @throws IllegalArgumentException если расход некорректен
     */
    public synchronized void addExpense(Expense e) {
        logger.debug("Attempting to add expense: {}", e);

        if (e == null) {
            logger.error("Failed to add expense: object is null");
            throw new IllegalArgumentException("Expense is null");
        }

        if (e.getAmount() <= 0) {
            logger.error("Failed to add expense: invalid amount {}", e.getAmount());
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (e.getCategory() == null || !CATEGORIES.contains(e.getCategory())) {
            logger.error("Failed to add expense: unknown category '{}'", e.getCategory());
            throw new IllegalArgumentException("Unknown category: " + e.getCategory());
        }

        expenses.add(e);
        logger.info("Expense added successfully: {}", e);
    }

    /**
     * Добавляет расход без выбрасывания исключений.
     * Если данные некорректны, запись просто пропускается.
     *
     * @param e расход
     */
    public synchronized void addExpenseSilently(Expense e) {
        try {
            addExpense(e);
        } catch (Exception ex) {
            logger.warn("addExpenseSilently: expense skipped: {} | reason: {}", e, ex.getMessage());
        }
    }

    /**
     * Получает копию списка всех расходов.
     *
     * @return список расходов
     */
    public synchronized List<Expense> getAllExpenses() {
        logger.debug("Retrieving all expenses (count = {})", expenses.size());
        return new ArrayList<>(expenses);
    }

    /**
     * Возвращает все расходы указанной категории.
     *
     * @param category категория
     * @return список расходов
     */
    public synchronized List<Expense> getByCategory(String category) {
        logger.debug("Filtering expenses by category '{}'", category);

        List<Expense> result = expenses.stream()
                .filter(e -> e.getCategory().equals(category))
                .collect(Collectors.toList());

        logger.info("Found {} expenses in category '{}'", result.size(), category);
        return result;
    }

    /**
     * Возвращает расходы за определённую дату.
     *
     * @param date дата
     * @return список расходов
     */
    public synchronized List<Expense> getByDate(LocalDate date) {
        logger.debug("Filtering expenses by date '{}'", date);

        List<Expense> result = expenses.stream()
                .filter(e -> e.getDate().equals(date))
                .collect(Collectors.toList());

        logger.info("Found {} expenses on date '{}'", result.size(), date);
        return result;
    }

    /**
     * Вычисляет общую сумму всех расходов.
     *
     * @return сумма расходов
     */
    public synchronized double getTotal() {
        double total = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        logger.debug("Total expenses amount: {}", total);
        return total;
    }

    /**
     * Группирует расходы по категориям и вычисляет сумму по каждой.
     *
     * @return карта категория → сумма
     */
    public synchronized Map<String, Double> getTotalByCategory() {
        logger.debug("Calculating totals by category...");

        Map<String, Double> map = new LinkedHashMap<>();
        for (String c : CATEGORIES) map.put(c, 0.0);

        for (Expense e : expenses) {
            map.put(e.getCategory(),
                    map.getOrDefault(e.getCategory(), 0.0) + e.getAmount());
        }

        logger.info("Totals by category calculated");
        return map;
    }

    /**
     * Подсчитывает процент расходов по категории.
     * Если общая сумма равна нулю, возвращает 0.
     *
     * @param category категория
     * @return процент от общей суммы (0–100)
     */
    public synchronized double getPercentage(String category) {
        double total = getTotal();
        if (total == 0) {
            logger.warn("Percentage calculation: total = 0, returning 0%");
            return 0.0;
        }

        double catTotal = getTotalByCategory().getOrDefault(category, 0.0);
        double pct = Math.round((catTotal / total) * 10000.0) / 100.0;

        logger.debug("Category '{}' percentage = {}%", category, pct);
        return pct;
    }
}
