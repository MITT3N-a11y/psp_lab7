package by.charity.shared.dto;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS, ERROR, UNAUTHORIZED, FORBIDDEN, NOT_FOUND
    }

    private Status status;
    private String message;
    private Object data;

    public Response() {}

    public Response(Status status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static Response success(Object data) {
        return new Response(Status.SUCCESS, "OK", data);
    }

    public static Response success(String message, Object data) {
        return new Response(Status.SUCCESS, message, data);
    }

    public static Response error(String message) {
        return new Response(Status.ERROR, message, null);
    }

    public static Response unauthorized() {
        return new Response(Status.UNAUTHORIZED, "Не авторизован", null);
    }

    public static Response forbidden() {
        return new Response(Status.FORBIDDEN, "Нет прав доступа", null);
    }

    public boolean isSuccess() { return status == Status.SUCCESS; }

    // Getters and setters
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}