package br.com.sigos.controller;

import br.com.sigos.model.TipoServico;
import br.com.sigos.service.TipoServicoService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Set;

public class TipoServicoController implements HttpHandler {
    private static final String ROTA = "/api/tipos-servico";
    private final TipoServicoService service;


    public TipoServicoController(TipoServicoService service) {
        this.service = service;
    }

    @Override

    public void handle(HttpExchange exchange) throws IOException {
        try {
            Long codigo = HttpUtil.codigoDaRota(exchange, ROTA);
            String metodo = exchange.getRequestMethod();

            if (metodo.equals("GET")) {
                if (codigo == null) {
                    HttpUtil.responder(exchange, 200, service.listar());
                } else {
                    HttpUtil.responder(exchange, 200, service.buscarPorCodigo(codigo));
                }
            } else if (metodo.equals("POST") && codigo == null) {
                JsonObject json = HttpUtil.lerJson(exchange);
                TipoServico entidade = lerEntidade(null, json);
                TipoServico salvo = service.cadastrar(entidade);
                exchange.getResponseHeaders().set("Location", ROTA + "/" + salvo.getCodigo());
                HttpUtil.responder(exchange, 201, salvo);
            } else if (metodo.equals("PUT") && codigo != null) {
                JsonObject json = HttpUtil.lerJson(exchange);
                TipoServico entidade = lerEntidade(codigo, json);
                HttpUtil.responder(exchange, 200, service.atualizar(codigo, entidade));
            } else if (metodo.equals("DELETE") && codigo != null) {
                service.excluir(codigo);
                HttpUtil.responder(exchange, 204, null);
            } else {
                String valorPermitido = codigo == null ? "GET, POST" : "GET, PUT, DELETE";
                exchange.getResponseHeaders().set("Allow", valorPermitido);
                HttpUtil.erro(exchange, 405, "Método não permitido.");
            }
        } catch (Exception e) {
            HttpUtil.tratarErro(exchange, e);
        } finally {
            exchange.close();
        }
    }


    private TipoServico lerEntidade(Long codigo, JsonObject json) {
        HttpUtil.validarCampo(json, Set.of("codigoCategoria", "nome", "descricao"));
        long codigoCategoria = HttpUtil.codigo(json, "codigoCategoria");
        String nome = HttpUtil.texto(json, "nome");
        String descricao = HttpUtil.texto(json, "descricao");
        return new TipoServico(codigo, codigoCategoria, nome, descricao);
    }
}
