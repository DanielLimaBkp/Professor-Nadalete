package br.com.sigos.controller;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class StatusController implements HttpHandler {
    private final Gson gson = new Gson();
    @Override public void handle(HttpExchange exchange) throws IOException {
        int codigo;
        Object corpo;
        if (!exchange.getRequestURI().getPath().equals("/api/status")) {
            codigo = 404;
            corpo = Map.of("erro", "Rota não encontrada.");
        } else if (!exchange.getRequestMethod().equals("GET")) {
            codigo = 405;
            exchange.getResponseHeaders().set("Allow", "GET");
            corpo = Map.of("erro", "Método não permitido.");
        } else {
            codigo = 200;
            corpo = Map.of("aplicacao", "SIGOS", "status", "online", "etapa", "CRUD de cadastros");
        }
        byte[] bytes = gson.toJson(corpo).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(codigo, bytes.length);
        try (var saida = exchange.getResponseBody()) { saida.write(bytes); }
        finally { exchange.close(); }
    }
}
