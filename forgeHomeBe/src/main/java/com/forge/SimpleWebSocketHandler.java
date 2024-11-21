package com.forge;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SimpleWebSocketHandler extends TextWebSocketHandler {
    // Колекції для збереження сесій WebSocket
    // picoSessions - сесії, пов'язані з Raspberry Pi Pico
    // sessions - сесії для інших клієнтів
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, WebSocketSession> picoSessions = new ConcurrentHashMap<>();

    // Використовуємо ExecutorService для виконання задач у віртуальних потоках
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();

    // Метод, який викликається після встановлення з'єднання з WebSocket
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Отримуємо ідентифікатор клієнта з заголовків запиту
        String clientId = session.getHandshakeHeaders().getFirst("client-id");

        if (clientId != null) {
            // Якщо є client-id, зберігаємо сесію в picoSessions
            picoSessions.put(clientId, session);
        } else {
            // Якщо client-id немає у заголовках, пробуємо отримати його з параметрів URI
            clientId = session.getUri().getQuery();
            if (clientId != null && clientId.contains("client-id=")) {
                clientId = clientId.split("=")[1];
            }

            // Якщо client-id знайдений, зберігаємо сесію в sessions
            if (clientId != null) {
                sessions.put(clientId, session);
            }
        }
    }

    // Метод, який викликається після закриття з'єднання WebSocket
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // Отримуємо client-id з заголовків
        String clientId = session.getHandshakeHeaders().getFirst("client-id");

        if (clientId != null) {
            // Якщо client-id є, видаляємо сесію з picoSessions
            picoSessions.remove(clientId);
        } else {
            // Якщо client-id немає в заголовках, намагаємось отримати його з параметрів URI
            clientId = session.getUri().getQuery();
            if (clientId != null && clientId.contains("client-id=")) {
                clientId = clientId.split("=")[1];
            }

            // Якщо client-id знайдений, видаляємо сесію з sessions
            if (clientId != null) {
                sessions.remove(clientId);
            }
        }
    }

    // Метод, який обробляє текстові повідомлення, отримані від клієнта
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Обробка повідомлення виконується у віртуальному потоці для зменшення навантаження на основний потік
//        virtualThreadExecutor.execute(() -> {
        threadExecutor.execute(() -> {
            try {
                // Отримуємо client-id з заголовків або URI
                String clientId = session.getHandshakeHeaders().getFirst("client-id");

                if (clientId != null) {
                    // Якщо знайдений client-id, знаходимо сесію відповідного клієнта в sessions
                    WebSocketSession clientSession = sessions.get(clientId);
                    if (clientSession != null && clientSession.isOpen()) {
                        // Якщо сесія відкрита, відправляємо повідомлення від pico до Angular
                        clientSession.sendMessage(new TextMessage("From pico to angular " + clientId + ": "
                                + message.getPayload()));
                    }
                } else {
                    // Якщо client-id не знайдений в заголовках, намагаємось отримати його з URI
                    clientId = session.getUri().getQuery();
                    if (clientId != null && clientId.contains("client-id=")) {
                        clientId = clientId.split("=")[1];
                    }

                    if (clientId != null) {
                        // Якщо client-id знайдений, обробляємо повідомлення та відправляємо його в сесію pico
                        System.out.println("Message from client " + clientId + ": " + message.getPayload());

                        WebSocketSession clientSession = picoSessions.get(clientId);

                        if (clientSession != null && clientSession.isOpen()) {
                            // Якщо сесія відкрито, відправляємо повідомлення від Angular до pico
                            clientSession.sendMessage(new TextMessage("From angular to pico " + clientId + ": "
                                    + message.getPayload()));
                        }
                    }
                }
            } catch (Exception e) {
                // Обробка виключень при обробці повідомлень
                System.err.println("Error handling message: " + e.getMessage());
            }
        });
    }
}
