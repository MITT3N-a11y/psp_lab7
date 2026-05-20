package by.charity.shared.exception;

public class CharityException extends RuntimeException {
    private final String code;

    public CharityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CharityException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() { return code; }

    // Subclasses
    public static class ValidationException extends CharityException {
        public ValidationException(String message) {
            super("VALIDATION_ERROR", message);
        }
    }

    public static class NotFoundException extends CharityException {
        public NotFoundException(String entity, Long id) {
            super("NOT_FOUND", entity + " с ID " + id + " не найден");
        }
    }

    public static class AuthException extends CharityException {
        public AuthException(String message) {
            super("AUTH_ERROR", message);
        }
    }

    public static class DatabaseException extends CharityException {
        public DatabaseException(String message, Throwable cause) {
            super("DB_ERROR", message, cause);
        }
    }
}