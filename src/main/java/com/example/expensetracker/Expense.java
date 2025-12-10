package com.example.expensetracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDate;

/**
 * Класс представляет одну запись о расходе.
 * Содержит сумму, категорию, дату и необязательное описание.
 * Используется в менеджере расходов, сохранении данных и экспорте.
 */
public class Expense {

    private static final Logger logger = LogManager.getLogger(Expense.class);

    private double amount;
    private String category;
    private LocalDate date;
    private String description;

    /**
     * Конструктор без параметров.
     * Создаёт пустой объект расхода.
     */
    public Expense() {
        logger.debug("Created empty Expense()");
    }

    /**
     * Конструктор создания расхода.
     *
     * @param amount      сумма расхода
     * @param category    категория
     * @param date        дата расхода
     * @param description описание расхода (может быть null)
     */
    public Expense(double amount, String category, LocalDate date, String description) {
        logger.debug("Creating Expense(amount={}, category={}, date={}, desc={})",
                amount, category, date, description);

        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    /**
     * Альтернативный конструктор, используется в Persistence/MainApp.
     *
     * @param category    категория
     * @param amount      сумма расхода
     * @param date        дата
     * @param description описание (может быть null)
     */
    public Expense(String category, double amount, LocalDate date, String description) {
        logger.debug("Creating Expense(category={}, amount={}, date={}, desc={})",
                category, amount, date, description);

        this.amount = amount;
        this.category = category;
        this.date = date;
        this.description = description;
    }

    /**
     * @return сумма расхода
     */
    public double getAmount() {
        return amount;
    }

    /**
     * Изменяет сумму расхода.
     *
     * @param amount новая сумма (должна быть положительной)
     */
    public void setAmount(double amount) {
        if (amount <= 0) {
            logger.error("Attempt to set invalid amount: {}", amount);
        } else {
            logger.debug("Amount changed: {} -> {}", this.amount, amount);
        }
        this.amount = amount;
    }

    /**
     * @return категория расхода
     */
    public String getCategory() {
        return category;
    }

    /**
     * Изменяет категорию расхода.
     *
     * @param category новая категория
     */
    public void setCategory(String category) {
        logger.debug("Category changed: {} -> {}", this.category, category);
        this.category = category;
    }

    /**
     * @return дата расхода
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Устанавливает новую дату расхода.
     *
     * @param date дата
     */
    public void setDate(LocalDate date) {
        logger.debug("Date changed: {} -> {}", this.date, date);
        this.date = date;
    }

    /**
     * @return описание расхода (может быть пустым или null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Изменяет описание расхода.
     *
     * @param description новое описание
     */
    public void setDescription(String description) {
        logger.debug("Description changed: '{}' -> '{}'", this.description, description);
        this.description = description;
    }

    /**
     * Возвращает строковое представление расхода.
     *
     * @return строка формата "date | category | amount | description"
     */
    @Override
    public String toString() {
        return String.format(
                "%s | %s | %.2f | %s",
                date,
                category,
                amount,
                description == null ? "" : description
        );
    }
}
