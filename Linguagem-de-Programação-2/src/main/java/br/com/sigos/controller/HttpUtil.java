package br.com.sigos.controller;

import br.com.sigos.model.Pessoa;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonPrimitive;
import java.time.LocalDateTime;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class HttpUtil {
    private static final Gson GSON = new GsonBuilder().serializeNulls()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (data, tipo, contexto) -> new JsonPrimitive(data.toString())).create();
    private static final Gson JSON_ESTRITO = new GsonBuilder().setStrictness(Strictness.STRICT).create();


    private HttpUtil() {}


    public static Long codigoDaRota(HttpExchange exchange, String rota) {
        String caminho = exchange.getRequestURI().getPath();
        if (caminho.equals(rota) || caminho.equals(rota + "/")) {
            return null;
        }
        if (!caminho.startsWith(rota + "/")) {
            throw new NoSuchElementException("Rota não encontrada.");
        }
        String restante = caminho.substring(rota.length() + 1);
        if (restante.contains("/")) {
            throw new NoSuchElementException("Rota não encontrada.");
        }
        return lerCodigo(restante);
    }


    public static long lerCodigo(String valor) {
        if (!valor.matches("[0-9]+")) {
            throw new IllegalArgumentException("Código deve ser inteiro positivo.");
        }
        try {
            long codigo = Long.parseLong(valor);
            if (codigo <= 0) {
                throw new IllegalArgumentException("Código deve ser positivo.");
            }
            return codigo;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Código fora do intervalo permitido.");
        }
    }


    public static Long filtroCodigo(HttpExchange exchange, String nome) {
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) return null;
        String[] listagemSegmentoRota = query.split("=", -1);
        if (listagemSegmentoRota.length != 2 || !listagemSegmentoRota[0].equals(nome))
            throw new IllegalArgumentException("Filtro esperado: " + nome + "=codigo.");
        return lerCodigo(listagemSegmentoRota[1]);
    }


    public static JsonObject lerJson(HttpExchange exchange) throws IOException {
        String tipo = exchange.getRequestHeaders().getFirst("Content-Type");
        if (tipo == null || !tipo.split(";")[0].strip().equalsIgnoreCase("application/json")) {
            throw new IllegalArgumentException("Use Content-Type: application/json.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(65537);
        if (bytes.length > 65536) {
            throw new IllegalArgumentException("Corpo excede 64 KiB.");
        }
        String corpo = new String(bytes, StandardCharsets.UTF_8);
        JsonElement json = JSON_ESTRITO.fromJson(corpo, JsonElement.class);
        if (json == null || !json.isJsonObject()) {
            throw new IllegalArgumentException("Envie um objeto JSON.");
        }
        return json.getAsJsonObject();
    }


    public static void validarCampo(JsonObject json, Set<String> valorPermitido) {
        for (String campo : json.keySet()) {
            if (!valorPermitido.contains(campo)) {
                throw new IllegalArgumentException("Campo não permitido: " + campo);
            }
        }
    }


    public static String texto(JsonObject json, String campo) {
        JsonElement valor = json.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(campo + " deve ser texto.");
        }
        return valor.getAsString();
    }


    public static long codigo(JsonObject json, String campo) {
        JsonElement valor = json.get(campo);
        if (valor == null || !valor.isJsonPrimitive() || !valor.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(campo + " deve ser inteiro positivo.");
        }
        return lerCodigo(valor.getAsString());
    }


    private static JsonElement serializar(Object corpo) {
        if (corpo instanceof List<?> lista) {
            JsonArray array = new JsonArray();
            for (Object item : lista) {
                array.add(serializar(item));
            }
            return array;
        }
        JsonElement json = GSON.toJsonTree(corpo);
        if (corpo instanceof Pessoa pessoa) {
            json.getAsJsonObject().addProperty("papel", pessoa.getPapel());
        }
        return json;
    }


    public static void responder(HttpExchange exchange, int status, Object corpo) throws IOException {
        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
            return;
        }
        byte[] bytes = GSON.toJson(serializar(corpo)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var saida = exchange.getResponseBody()) {
            saida.write(bytes);
        }
    }


    public static void erro(HttpExchange exchange, int status, String mensagem) throws IOException {
        responder(exchange, status, Map.of("erro", mensagem));
    }

    public static void metodoNaoPermitido(HttpExchange exchange, String valorPermitido) throws IOException {
        exchange.getResponseHeaders().set("Allow", valorPermitido);
        erro(exchange, 405, "Método não permitido.");
    }


    public static void tratarErro(HttpExchange exchange, Exception e) throws IOException {
        if (e instanceof NoSuchElementException) {
            erro(exchange, 404, e.getMessage());
        } else if (e instanceof JsonParseException) {
            erro(exchange, 400, "JSON inválido.");
        } else if (e instanceof IllegalArgumentException) {
            erro(exchange, 400, e.getMessage());
        } else if (e instanceof IllegalStateException) {
            erro(exchange, 409, e.getMessage());
        } else if (e instanceof SQLException sql && (sql.getErrorCode() & 255) == 19) {
            
            erro(exchange, 409, "Dados duplicados ou registro vinculado a outro cadastro.");
        } else {
            e.printStackTrace();
            erro(exchange, 500, "Erro interno.");
        }
    }
}
