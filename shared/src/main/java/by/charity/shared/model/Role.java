package by.charity.shared.model;

import java.io.Serializable;

public enum Role implements Serializable {
    ADMIN("Администратор"),
    MANAGER("Менеджер"),
    ACCOUNTANT("Бухгалтер"),
    GUEST("Гость");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}