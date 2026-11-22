package br.com.sigos.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Set;

public class PaginaController implements HttpHandler {
    private static final Set<String> ARQUIVO_PUBLICO = Set.of(
            "index.html", "clientes.html", "tecnicos.html", "categorias.html", "tipos-servico.html",
            "style.css", "api.js", "clientes.js", "tecnicos.js", "categorias.js", "tipos-servico.js",
            "ordens.html", "ordens.js", "ordem.html", "ordem.js");

    @Override

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = exchange.getRequestURI().getPath();
            String arquivo = caminho.equals("/") ? "index.html" : caminho.substring(1);
            if (!ARQUIVO_PUBLICO.contains(arquivo)) {
                HttpUtil.erro(exchange, 404, "Página não encontrada.");
                return;
            }
            String metodo = exchange.getRequestMethod();
            if (!metodo.equals("GET") && !metodo.equals("HEAD")) {
                exchange.getResponseHeaders().set("Allow", "GET, HEAD");
                HttpUtil.erro(exchange, 405, "Método não permitido.");
                return;
            }
            try (var recurso = getClass().getResourceAsStream("/static/" + arquivo)) {
                if (recurso == null) {
                    HttpUtil.erro(exchange, 404, "Página não encontrada.");
                    return;
                }
                String tipo = "text/html";
                if (arquivo.endsWith(".css")) tipo = "text/css";
                if (arquivo.endsWith(".js")) tipo = "text/javascript";
                byte[] bytes = recurso.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", tipo + "; charset=UTF-8");
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                if (metodo.equals("HEAD")) {
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            }
        } finally {
            exchange.close();
        }
    }
}
