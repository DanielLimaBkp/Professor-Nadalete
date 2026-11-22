package br.com.sigos.controller;

import br.com.sigos.model.*;
import br.com.sigos.service.OrdemServicoService;
import br.com.sigos.service.AtividadeService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.*;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Set;

public class OrdemServicoController implements HttpHandler {
    private static final String ROTA = "/api/ordens";
    private final OrdemServicoService service;
    private final AtividadeService atividadeService;

    public OrdemServicoController(OrdemServicoService service, AtividadeService atividadeService) {
        this.service = service;
        this.atividadeService = atividadeService;
    }
    @Override public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = exchange.getRequestURI().getPath();
            String metodo = exchange.getRequestMethod();
            if (caminho.equals(ROTA) || caminho.equals(ROTA + "/")) {
                if (metodo.equals("GET")) {
                    HttpUtil.responder(exchange, 200, service.listar(HttpUtil.filtroCodigo(exchange, "codigoCliente")));
                } else if (metodo.equals("POST")) {
                    OrdemServico salva = service.cadastrar(lerOrdem(null, HttpUtil.lerJson(exchange)));
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
                else if (metodo.equals("PUT")) HttpUtil.responder(exchange, 200, service.atualizar(codigo, lerOrdem(codigo, HttpUtil.lerJson(exchange))));
                else if (metodo.equals("DELETE")) {
                    service.excluir(codigo);
                    HttpUtil.responder(exchange, 204, null);
                } else HttpUtil.metodoNaoPermitido(exchange, "GET, PUT, DELETE");
            } else if (listagemSegmentoRota.length == 2 && listagemSegmentoRota[1].equals("status")) {
                if (!metodo.equals("PATCH")) { HttpUtil.metodoNaoPermitido(exchange, "PATCH"); return; }
                JsonObject json = HttpUtil.lerJson(exchange);
                HttpUtil.validarCampo(json, Set.of("status"));
                StatusOrdem status;
                try { status = StatusOrdem.valueOf(HttpUtil.texto(json, "status")); }
                catch (IllegalArgumentException e) { throw new IllegalArgumentException("Status de ordem inválido."); }
                HttpUtil.responder(exchange, 200, service.alterarStatus(codigo, status));
            } else if (listagemSegmentoRota.length == 2 && listagemSegmentoRota[1].equals("atividades")) {
                if (metodo.equals("GET")) HttpUtil.responder(exchange, 200, atividadeService.listar(codigo));
                else if (metodo.equals("POST")) {
                    JsonObject json = HttpUtil.lerJson(exchange);
                    HttpUtil.validarCampo(json, Set.of("descricao"));
                    Atividade salva = atividadeService.cadastrar(new Atividade(null, codigo, HttpUtil.texto(json, "descricao")));
                    exchange.getResponseHeaders().set("Location", "/api/atividades/" + salva.getCodigo());
                    HttpUtil.responder(exchange, 201, salva);
                } else HttpUtil.metodoNaoPermitido(exchange, "GET, POST");
            } else if (listagemSegmentoRota.length == 2 && listagemSegmentoRota[1].equals("tecnicos")) {
                if (metodo.equals("GET")) HttpUtil.responder(exchange, 200, service.listarTecnicoResponsavel(codigo));
                else HttpUtil.metodoNaoPermitido(exchange, "GET");
            } else if (listagemSegmentoRota.length == 3 && listagemSegmentoRota[1].equals("tecnicos")) {
                long codigoTecnico = HttpUtil.lerCodigo(listagemSegmentoRota[2]);
                if (metodo.equals("PUT")) {
                    service.associarTecnico(codigo, codigoTecnico);
                    HttpUtil.responder(exchange, 204, null);
                } else if (metodo.equals("DELETE")) {
                    service.removerTecnico(codigo, codigoTecnico);
                    HttpUtil.responder(exchange, 204, null);
                } else HttpUtil.metodoNaoPermitido(exchange, "PUT, DELETE");
            } else throw new NoSuchElementException("Rota não encontrada.");
        } catch (Exception e) { HttpUtil.tratarErro(exchange, e); }
        finally { exchange.close(); }
    }

    private OrdemServico lerOrdem(Long codigo, JsonObject json) {
        HttpUtil.validarCampo(json, Set.of("codigoCliente", "codigoTipoServico", "titulo", "descricao", "prioridade"));
        Prioridade prioridade;
        try { prioridade = Prioridade.valueOf(HttpUtil.texto(json, "prioridade")); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Prioridade deve ser BAIXA, MEDIA ou ALTA."); }
        return new OrdemServico(codigo, HttpUtil.codigo(json, "codigoCliente"), HttpUtil.codigo(json, "codigoTipoServico"),
                HttpUtil.texto(json, "titulo"), HttpUtil.texto(json, "descricao"), prioridade);
    }
}
