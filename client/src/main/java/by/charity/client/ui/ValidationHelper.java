package by.charity.client.ui;

import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class ValidationHelper {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9\\s\\-()]{7,20}$");

    private static final Pattern MONEY_PATTERN =
            Pattern.compile("^\\d{1,10}([.,]\\d{1,2})?$");

    // Проверить что поле не пустое
    public static boolean validateRequired(TextField field, Label errorLabel, String fieldName) {
        if (field.getText() == null || field.getText().trim().isEmpty()) {
            setError(field, errorLabel, fieldName + " обязательно для заполнения");
            return false;
        }
        clearError(field, errorLabel);
        return true;
    }

    // Проверить email
    public static boolean validateEmail(TextField field, Label errorLabel) {
        String val = field.getText().trim();
        if (val.isEmpty()) {
            clearError(field, errorLabel);
            return true; // email необязательный
        }
        if (!EMAIL_PATTERN.matcher(val).matches()) {
            setError(field, errorLabel, "Введите корректный email (например: user@mail.ru)");
            return false;
        }
        clearError(field, errorLabel);
        return true;
    }

    // Проверить телефон
    public static boolean validatePhone(TextField field, Label errorLabel) {
        String val = field.getText().trim();
        if (val.isEmpty()) {
            clearError(field, errorLabel);
            return true; // телефон необязательный
        }
        if (!PHONE_PATTERN.matcher(val).matches()) {
            setError(field, errorLabel, "Введите корректный телефон (например: +375291234567)");
            return false;
        }
        clearError(field, errorLabel);
        return true;
    }

    // Проверить сумму денег
    public static boolean validateMoney(TextField field, Label errorLabel, String fieldName) {
        String val = field.getText().trim();
        if (val.isEmpty()) {
            setError(field, errorLabel, fieldName + " обязательно для заполнения");
            return false;
        }
        String normalized = val.replace(',', '.');
        if (!MONEY_PATTERN.matcher(val).matches()) {
            setError(field, errorLabel, "Введите корректную сумму (например: 100 или 100,50)");
            return false;
        }
        try {
            BigDecimal amount = new BigDecimal(normalized);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                setError(field, errorLabel, "Сумма должна быть больше 0");
                return false;
            }
        } catch (NumberFormatException e) {
            setError(field, errorLabel, "Введите корректную сумму");
            return false;
        }
        clearError(field, errorLabel);
        return true;
    }

    // Проверить пароль
    public static boolean validatePassword(TextField field, Label errorLabel) {
        String val = field.getText();
        if (val == null || val.length() < 6) {
            setError(field, errorLabel, "Пароль должен содержать минимум 6 символов");
            return false;
        }
        clearError(field, errorLabel);
        return true;
    }

    // Проверить совпадение паролей
    public static boolean validatePasswordMatch(TextField pass1, TextField pass2, Label errorLabel) {
        if (!pass1.getText().equals(pass2.getText())) {
            setError(pass2, errorLabel, "Пароли не совпадают");
            return false;
        }
        clearError(pass2, errorLabel);
        return true;
    }

    // Получить BigDecimal из поля суммы
    public static BigDecimal parseMoney(TextField field) {
        return new BigDecimal(field.getText().trim().replace(',', '.'));
    }

    // Разрешить вводить только цифры и запятую/точку
    public static void applyMoneyFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9.,]*")) {
                field.setText(newVal.replaceAll("[^0-9.,]", ""));
            }
            // Не более одной точки/запятой
            long dots = newVal.chars().filter(c -> c == '.' || c == ',').count();
            if (dots > 1) {
                field.setText(oldVal);
            }
        });
    }

    // Разрешить вводить только цифры
    public static void applyDigitsOnlyFilter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("[0-9]*")) {
                field.setText(newVal.replaceAll("[^0-9]", ""));
            }
        });
    }

    // Ограничить длину поля
    public static void applyMaxLength(TextField field, int maxLength) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > maxLength) {
                field.setText(oldVal);
            }
        });
    }

    private static void setError(TextField field, Label errorLabel, String message) {
        field.setStyle(StyleManager.textFieldError());
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setTextFill(Color.web(StyleManager.DANGER));
            errorLabel.setVisible(true);
        }
    }

    private static void clearError(TextField field, Label errorLabel) {
        field.setStyle(StyleManager.textFieldStyle());
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }
}