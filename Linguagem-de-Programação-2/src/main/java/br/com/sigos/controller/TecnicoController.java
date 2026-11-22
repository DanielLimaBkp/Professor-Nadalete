package br.com.sigos.controller;

import br.com.sigos.model.Tecnico;
import br.com.sigos.service.TecnicoService;
import br.com.sigos.service.TecnicoTipoServicoService;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Set;

public class TecnicoController implements HttpHandler {
    private static final String ROTA = "/api/tecnicos";
    private final TecnicoService service;
    private final TecnicoTipoServicoService tecnicoTipoServicoService;


    public TecnicoController(TecnicoService service, TecnicoTipoServicoService tecnicoTipoServicoService) {
        this.service = service;
        this.tecnicoTipoServicoService = tecnicoTipoServicoService;
    }

    @Override

    public void handle(HttpExchange exchange) throws IOException {
        try {
            String caminho = exchange.getRequestURI().getPath();
            if (caminho.startsWith(ROTA + "/")) {
                String[] listagemSegmentoRota = caminho.substring(ROTA.length() + 1).split("/", -1);
                if (listagemSegmentoRota.length >= 2 && listagemSegmentoRota[1].equals("tipos-servico")) {
                    long codigoTecnico = HttpUtil.lerCodigo(listagemSegmentoRota[0]);
                    String metodo = exchange.getRequestMethod();
                    if (listagemSegmentoRota.length == 2) {
                        if (metodo.equals("GET")) HttpUtil.responder(exchange, 200, tecnicoTipoServicoService.listarTipoServicoHabilitado(codigoTecnico));
                        else HttpUtil.metodoNaoPermitido(exchange, "GET");
                    } else if (listagemSegmentoRota.length == 3) {
                        long codigoTipoServico = HttpUtil.lerCodigo(listagemSegmentoRota[2]);
                        if (metodo.equals("PUT")) {
                            tecnicoTipoServicoService.associar(codigoTecnico, codigoTipoServico);
                            HttpUtil.responder(exchange, 204, null);
                        } else if (metodo.equals("DELETE")) {
                            tecnicoTipoServicoService.remover(codigoTecnico, codigoTipoServico);
                            HttpUtil.responder(exchange, 204, null);
                        } else HttpUtil.metodoNaoPermitido(exchange, "PUT, DELETE");
                    } else throw new java.util.NoSuchElementException("Rota não encontrada.");
                    return;
                }
            }
            Long codigo = HttpUtil.codigoDaRota(exchange, ROTA);
            String metodo = exchange.getRequestMethod();

            if (metodo.equals("GET")) {
                if (codigo == null) {
                    Long codigoTipoServico = HttpUtil.filtroCodigo(exchange, "codigoTipoServico");
                    HttpUtil.responder(exchange, 200, codigoTipoServico == null ? service.listar() : tecnicoTipoServicoService.listarTecnicoHabilitado(codigoTipoServico));
                } else {
                    HttpUtil.responder(exchange, 200, service.buscarPorCodigo(codigo));
                }
            } else if (metodo.equals("POST") && codigo == null) {
                JsonObject json = HttpUtil.lerJson(exchange);
                Tecnico entidade = lerEntidade(null, json);
                Tecnico salvo = service.cadastrar(entidade);
                exchange.getResponseHeaders().set("Location", ROTA + "/" + salvo.getCodigo());
                HttpUtil.responder(exchange, 201, salvo);
            } else if (metodo.equals("PUT") && codigo != null) {
                JsonObject json = HttpUtil.lerJson(exchange);
                Tecnico entidade = lerEntidade(codigo, json);
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


    private Tecnico lerEntidade(Long codigo, JsonObject json) {
        HttpUtil.validarCampo(json, Set.of("nome", "email", "especialidade"));
        String nome = HttpUtil.texto(json, "nome");
        String email = HttpUtil.texto(json, "email");
        String especialidade = HttpUtil.texto(json, "especialidade");
        return new Tecnico(codigo, nome, email, especialidade);
    }
}
