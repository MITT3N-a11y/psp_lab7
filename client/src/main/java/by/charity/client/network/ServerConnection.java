package by.charity.client.network;

import by.charity.shared.dto.Request;
import by.charity.shared.dto.Response;

import java.io.*;
import java.net.Socket;

public class ServerConnection implements AutoCloseable {
    private static ServerConnection instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String host = "localhost";
    private int port = 8080;
    private String sessionToken;

    private ServerConnection() {}


    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    public void connect(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    public synchronized Response send(Request request) throws IOException {
        if (socket == null || socket.isClosed()) {
            connect(host, port);
        }
        request.setSessionToken(sessionToken);
        try {
            out.writeObject(request);
            out.flush();
            out.reset();
            return (Response) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Неверный формат ответа от сервера", e);
        }
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    @Override
    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}