package br.com.sigos.controller;

import br.com.sigos.model.Atividade;
import br.com.sigos.model.StatusAtividade;
import br.com.sigos.service.AtividadeService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.util.Set;
import java.util.NoSuchElementException;

public class AtividadeController implements HttpHandler {
    private static final String ROTA = "/api/atividades";
    private final AtividadeService service;

    public AtividadeController(AtividadeService service) { this.service = service; }
    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            if (caminho.equals(ROTA) || caminho.equals(ROTA + "/")) {
                if (metodo.equals("GET")) {
                    HttpUtil.responder(exchange, 200, service.listar(HttpUtil.filtroCodigo(exchange, "codigoOrdem")));
                } else if (metodo.equals("POST")) {
                    JsonObject json = HttpUtil.lerJson(exchange);
                    HttpUtil.validarCampo(json, Set.of("codigoOrdem", "descricao"));
                    Atividade salva = service.cadastrar(new Atividade(null, HttpUtil.codigo(json, "codigoOrdem"), HttpUtil.texto(json, "descricao")));
                    exchange.getResponseHeaders().set("Location", ROTA + "/" + salva.getCodigo());
                    HttpUtil.responder(exchange, 201, salva);
                } else HttpUtil.metodoNaoPermitido(exchange, "GET, POST");
                return;
            }
            if (!caminho.startsWith(ROTA + "/")) throw new NoSuchElementException("Rota não encontrada.");
            String[] listagemSegmentoRota = caminho.substring(ROTA.length() + 1).split("/", -1);
            long codigo = HttpUtil.lerCodigo(listagemSegmentoRota[0]);
            if (listagemSegmentoRota.length == 1) {
                if (metodo.equals("GET")) HttpUtil.responder(exchange, 200, service.buscarPorCodigo(codigo));
                else if (metodo.equals("PUT")) {
                    JsonObject json = HttpUtil.lerJson(exchange);
                    HttpUtil.validarCampo(json, Set.of("descricao"));
                    HttpUtil.responder(exchange, 200, service.atualizar(codigo, HttpUtil.texto(json, "descricao")));
                } else if (metodo.equals("DELETE")) {
                    service.excluir(codigo);
                    HttpUtil.responder(exchange, 204, null);
                } else HttpUtil.metodoNaoPermitido(exchange, "GET, PUT, DELETE");
            } else if (listagemSegmentoRota.length == 2 && listagemSegmentoRota[1].equals("status")) {
                if (!metodo.equals("PATCH")) { HttpUtil.metodoNaoPermitido(exchange, "PATCH"); return; }
                JsonObject json = HttpUtil.lerJson(exchange);
                HttpUtil.validarCampo(json, Set.of("status"));
                StatusAtividade status;
                try { status = StatusAtividade.valueOf(HttpUtil.texto(json, "status")); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Status de atividade inválido."); }
                HttpUtil.responder(exchange, 200, service.alterarStatus(codigo, status));
            } else throw new NoSuchElementException("Rota não encontrada.");
        } catch (Exception e) { HttpUtil.tratarErro(exchange, e); }
        finally { exchange.close(); }
    }
}
