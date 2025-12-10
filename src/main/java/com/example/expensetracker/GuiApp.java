package com.example.expensetracker;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;

/**
 * Графическое JavaFX приложение для управления расходами.
 * Предоставляет интерфейс для:
 * <ul>
 *     <li>добавления расходов</li>
 *     <li>просмотра данных в таблице</li>
 *     <li>фильтрации по категориям</li>
 *     <li>экспорта данных в Excel</li>
 *     <li>сохранения/загрузки данных через Persistence</li>
 *     <li>просмотра общей статистики (итог + проценты)</li>
 * </ul>
 */
public class GuiApp extends Application {

    /** Менеджер для всех операций с расходами. */
    private final ExpenseManager manager = new ExpenseManager();

    /** Класс для сохранения/загрузки данных. */
    private final Persistence persistence = new Persistence();

    /** Экспорт данных в Excel. */
    private final ExcelExporter excelExporter = new ExcelExporter();

    /** Наблюдаемая коллекция для отображения расходов в таблице. */
    private final ObservableList<Expense> observableExpenses =
            FXCollections.observableArrayList();

    /**
     * Главный метод JavaFX-приложения.
     * Создаёт все элементы интерфейса, загружает данные,
     * формирует таблицу, панель статистики и подключает обработчики.
     *
     * @param stage главное окно приложения
     */
    @Override
    public void start(Stage stage) {

        // -------- STATISTICS AREA --------
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefHeight(140);

        // -------- LOAD DATA ----------
        try {
            persistence.load().forEach(manager::addExpenseSilently);
            observableExpenses.addAll(manager.getAllExpenses());
        } catch (Exception e) {
            System.out.println("Load error: " + e.getMessage());
        }

        updateStatistics(statsArea);

        // -------- UI ELEMENTS --------

        ComboBox<String> categoryBox =
                new ComboBox<>(FXCollections.observableArrayList(ExpenseManager.CATEGORIES));
        categoryBox.setPromptText("Category");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        DatePicker datePicker = new DatePicker(LocalDate.now());

        TextField descField = new TextField();
        descField.setPromptText("Description");

        Button addBtn = new Button("Add");
        Button exportAllBtn = new Button("Export All");
        Button exportCatBtn = new Button("Export Category");
        Button saveBtn = new Button("Save");

        // -------- TABLE --------

        TableView<Expense> table = new TableView<>(observableExpenses);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Expense, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<Expense, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Expense, Double> colAmount = new TableColumn<>("Amount");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Expense, String> colDesc = new TableColumn<>("Description");
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.getColumns().addAll(colDate, colCat, colAmount, colDesc);

        // -------- BUTTON ACTIONS --------

        addBtn.setOnAction(a -> {
            try {
                String cat = categoryBox.getValue();
                if (cat == null) {
                    showAlert("Error", "Choose category");
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountField.getText());
                } catch (Exception ex) {
                    showAlert("Error", "Invalid amount");
                    return;
                }

                if (amount <= 0) {
                    showAlert("Error", "Amount must be positive");
                    return;
                }

                LocalDate date = datePicker.getValue();
                String desc = descField.getText();

                Expense e = new Expense(cat, amount, date, desc);
                manager.addExpense(e);
                observableExpenses.add(e);

                updateStatistics(statsArea);

                amountField.clear();
                descField.clear();
                categoryBox.setValue(null);

            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        exportAllBtn.setOnAction(a -> {
            try {
                excelExporter.exportAllExpenses("all_expenses.xlsx", manager.getAllExpenses());
                showAlert("Success", "Exported to all_expenses.xlsx");
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        exportCatBtn.setOnAction(a -> {
            String cat = categoryBox.getValue();
            if (cat == null) {
                showAlert("Error", "Choose category");
                return;
            }

            try {
                excelExporter.exportCategory(
                        "category_" + cat + ".xlsx",
                        manager.getByCategory(cat),
                        cat
                );
                showAlert("Success", "Exported category_" + cat + ".xlsx");
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        saveBtn.setOnAction(a -> {
            try {
                persistence.save(manager.getAllExpenses());
                updateStatistics(statsArea);
                showAlert("Saved", "Data saved to file");
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });


        // -------- LAYOUT --------

        HBox inputRow = new HBox(10, categoryBox, amountField, datePicker, descField, addBtn);
        inputRow.setPadding(new Insets(10));

        HBox bottomRow = new HBox(10, exportAllBtn, exportCatBtn, saveBtn);
        bottomRow.setPadding(new Insets(10));

        VBox root = new VBox(10, inputRow, statsArea, table, bottomRow);
        root.setPadding(new Insets(10));

        // -------- SCENE --------

        stage.setTitle("Expense Tracker");
        stage.setScene(new Scene(root, 900, 550));
        stage.show();
    }

    /**
     * Обновляет текстовое поле статистики:
     * <ul>
     *     <li>общая сумма расходов</li>
     *     <li>суммы по категориям</li>
     *     <li>проценты каждой категории</li>
     * </ul>
     *
     * @param statsArea текстовое поле для отображения статистики
     */
    private void updateStatistics(TextArea statsArea) {
        double total = manager.getTotal();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total spent: %.2f%n%n", total));

        var totals = manager.getTotalByCategory();
        for (String cat : ExpenseManager.CATEGORIES) {
            double sum = totals.get(cat);
            double pct = manager.getPercentage(cat);
            sb.append(String.format("%s: %.2f (%.2f%%)%n", cat, sum, pct));
        }

        statsArea.setText(sb.toString());
    }

    /**
     * Показывает всплывающее окно с сообщением.
     *
     * @param title заголовок окна
     * @param msg текст сообщения
     */
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    /**
     * Точка входа приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        launch();
    }
}
