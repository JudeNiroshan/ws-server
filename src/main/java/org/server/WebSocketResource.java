package org.server;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint("/connect/timer")
@ApplicationScoped
public class WebSocketResource {

    @ConfigProperty(name = "quarkus.application.version")
    String version;

    private Set<Session> sessions = new HashSet<>();
    private LocalDateTime localDateTime = LocalDateTime.now();

    String hostname = System.getenv("HOSTNAME") == null ? "localhost" : System.getenv("HOSTNAME");

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("onOpen> ");
        sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("onClose> ");
        sessions.remove(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("onError> : " + throwable);
        sessions.remove(session);
    }

    @Scheduled(every = "1s")
    public void emitSeconds() {
        String timestamp = String.valueOf(localDateTime);
        localDateTime = localDateTime.plusSeconds(1);

        JsonObject json = new JsonObject();
        json.put("timestamp", timestamp);
        json.put("value", Math.random());
        json.put("hostname", hostname);
        json.put("version", version);

        sendToAll(json.toString());
    }

    @OnMessage
    public void onMessage(String message, @PathParam("name") String name) {

        System.out.println("onMessage> " + name + ": " + message);
        localDateTime = LocalDateTime.parse(message).plusSeconds(1);
    }

    public void sendToAll(String message) {
        sessions.forEach(session -> {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
