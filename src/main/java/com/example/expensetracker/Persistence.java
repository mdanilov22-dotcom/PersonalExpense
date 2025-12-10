package com.example.expensetracker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс, отвечающий за сохранение и загрузку расходов в локальный текстовый файл.
 * <p>
 * Данные хранятся в простом текстовом формате,
 * где поля разделены точкой с запятой:
 * <pre>
 * дата;категория;сумма;описание
 * </pre>
 * Точки с запятой внутри поля описания экранируются как <code>\;</code>,
 * чтобы не конфликтовать с разделителем полей.
 * <p>
 * Этот класс отвечает исключительно за операции чтения и записи.
 * Проверка корректности данных осуществляется в {@link ExpenseManager}.
 */
public class Persistence {

    private static final Logger logger = LogManager.getLogger(Persistence.class);

    /** Имя файла, в котором хранятся данные. */
    private static final String FILE_NAME = "expenses.db";

    /**
     * Сохраняет список расходов в текстовый файл.
     * <p>
     * Каждый расход записывается в отдельную строку в формате:
     * <pre>
     * дата;категория;сумма;описание
     * </pre>
     * В описании точки с запятой экранируются как <code>\;</code>.
     *
     * @param expenses список расходов для сохранения; не должен быть {@code null}
     * @throws IOException если при записи файла произошла ошибка
     */
    public void save(List<Expense> expenses) throws IOException {

        logger.info("Сохранение {} расходов в файл: {}", expenses.size(), FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {

            for (Expense e : expenses) {
                // экранирование точек с запятой в описании
                String desc = e.getDescription() == null ? "" : e.getDescription().replace(";", "\\;");

                writer.write(
                        e.getDate() + ";" +
                                e.getCategory() + ";" +
                                e.getAmount() + ";" +
                                desc
                );
                writer.newLine();
            }

            logger.info("Успешно сохранено {} расходов", expenses.size());

        } catch (IOException ex) {
            logger.error("Ошибка при сохранении расходов: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Загружает список расходов из текстового файла.
     * <p>
     * Если файл ещё не существует, возвращается пустой список.
     * <p>
     * Каждая строка файла должна соответствовать формату:
     * <pre>
     * дата;категория;сумма;описание
     * </pre>
     * Точки с запятой, экранированные как <code>\;</code>, снова превращаются в обычные.
     * <p>
     * Ошибочные строки (некорректный формат или значения) пропускаются, при этом ошибка логируется.
     *
     * @return список корректно загруженных расходов (никогда не {@code null})
     * @throws IOException если при чтении файла произошла ошибка
     */
    public List<Expense> load() throws IOException {

        logger.info("Загрузка расходов из файла: {}", FILE_NAME);

        List<Expense> list = new ArrayList<>();
        File f = new File(FILE_NAME);

        if (!f.exists()) {
            logger.warn("Файл {} не найден, возвращается пустой список", FILE_NAME);
            return list;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                String[] parts = line.split(";", 4);

                if (parts.length < 4) {
                    logger.warn("Некорректная строка {}: {}", lineNumber, line);
                    continue;
                }

                try {
                    LocalDate date = LocalDate.parse(parts[0]);
                    String category = parts[1];
                    double amount = Double.parseDouble(parts[2]);

                    // восстановление экранированных точек с запятой
                    String desc = parts[3].replace("\\;", ";");

                    list.add(new Expense(category, amount, date, desc));

                } catch (Exception ex) {
                    logger.error(
                            "Ошибка обработки строки {}: '{}', причина: {}",
                            lineNumber, line, ex.getMessage()
                    );
                }
            }

            logger.info("Успешно загружено {} расходов", list.size());

        } catch (IOException ex) {
            logger.error("Ошибка чтения файла {}: {}", FILE_NAME, ex.getMessage());
            throw ex;
        }

        return list;
    }
}
