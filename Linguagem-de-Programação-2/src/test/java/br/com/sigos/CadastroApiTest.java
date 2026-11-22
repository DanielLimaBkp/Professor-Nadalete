package br.com.sigos;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.controller.*;
import br.com.sigos.repository.*;
import br.com.sigos.service.*;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.net.*;
import java.net.http.*;
import java.nio.file.Path;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

class CadastroApiTest {
    @TempDir Path pasta;

    private HttpServer servidor;

    private CarregarDados banco;

    private String base;
    private final HttpClient cliente = HttpClient.newHttpClient();

    @BeforeEach void preparar() throws Exception {
        banco = new CarregarDados(pasta.resolve("teste.db"));
        banco.inicializar();
        iniciar();
    }

    private void iniciar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var categoriaServicoRepository = new CategoriaServicoRepository(banco);
        servidor.createContext("/", new PaginaController());
        servidor.createContext("/api/status", new StatusController());
        servidor.createContext("/api/clientes", new ClienteController(new ClienteService(new ClienteRepository(banco))));
        servidor.createContext("/api/tecnicos", new TecnicoController(new TecnicoService(new TecnicoRepository(banco)), new TecnicoTipoServicoService(banco)));
        servidor.createContext("/api/categorias", new CategoriaServicoController(new CategoriaServicoService(categoriaServicoRepository)));
        servidor.createContext("/api/tipos-servico", new TipoServicoController(new TipoServicoService(new TipoServicoRepository(banco), categoriaServicoRepository)));
        AtividadeService atividadeService = new AtividadeService(banco);
        servidor.createContext("/api/ordens", new OrdemServicoController(new OrdemServicoService(banco), atividadeService));
        servidor.createContext("/api/atividades", new AtividadeController(atividadeService));
        servidor.start();
        base = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }
    @AfterEach void encerrar() { if (servidor != null) servidor.stop(0); }


    private HttpResponse<String> requisitar(String metodo, String rota, String corpo) throws Exception {
        var request = HttpRequest.newBuilder(URI.create(base + rota))
            .header("Content-Type", "application/json")
            .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo))
            .build();
        return cliente.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private long criar(String rota, String json) throws Exception {
        var resposta = requisitar("POST", rota, json);
        assertEquals(201, resposta.statusCode(), resposta.body());
        long codigo = JsonParser.parseString(resposta.body()).getAsJsonObject().get("codigo").getAsLong();
        assertEquals(rota + "/" + codigo, resposta.headers().firstValue("Location").orElseThrow());
        return codigo;
    }
    @Test void crudDosQuatroCadastros() throws Exception {
        long categoria = criar("/api/categorias", "{\"nome\":\"TI\",\"descricao\":\"Tecnologia\"}");
        String[] rotas = {"/api/clientes", "/api/tecnicos", "/api/tipos-servico"};
        String[] dadoFormulario = {
            "{\"nome\":\"Júlia\",\"email\":\"julia@exemplo.com\",\"documento\":\"123\",\"telefone\":\"11999999999\"}",
            "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"Redes\"}",
            "{\"codigoCategoria\":" + categoria + ",\"nome\":\"Suporte\",\"descricao\":\"Atendimento\"}"
        };
        for (int i = 0; i < rotas.length; i++) {
            long codigo = criar(rotas[i], dadoFormulario[i]);
            var consulta = requisitar("GET", rotas[i] + "/" + codigo, null);
            assertEquals(200, consulta.statusCode());
            JsonObject entidade = JsonParser.parseString(consulta.body()).getAsJsonObject();
            assertEquals(codigo, entidade.get("codigo").getAsLong());
            assertFalse(entidade.has("id"));
            if (i < 2) assertEquals(i == 0 ? "CLIENTE" : "TECNICO", entidade.get("papel").getAsString());
            assertEquals(1, JsonParser.parseString(requisitar("GET", rotas[i], null).body()).getAsJsonArray().size());
            JsonObject alteracao = JsonParser.parseString(dadoFormulario[i]).getAsJsonObject();
            alteracao.addProperty("nome", "Nome atualizado");
            var atualizacao = requisitar("PUT", rotas[i] + "/" + codigo, alteracao.toString());
            assertEquals(200, atualizacao.statusCode(), atualizacao.body());
            assertEquals("Nome atualizado", JsonParser.parseString(requisitar("GET", rotas[i] + "/" + codigo, null).body()).getAsJsonObject().get("nome").getAsString());
            var exclusao = requisitar("DELETE", rotas[i] + "/" + codigo, null);
            assertEquals(204, exclusao.statusCode());
            assertEquals("", exclusao.body());
            assertEquals(404, requisitar("GET", rotas[i] + "/" + codigo, null).statusCode());
        }
        var atualizacao = requisitar("PUT", "/api/categorias/" + categoria, "{\"nome\":\"Administrativo\",\"descricao\":\"Rotinas\"}");
        assertEquals(200, atualizacao.statusCode());
        assertEquals("Administrativo", JsonParser.parseString(requisitar("GET", "/api/categorias/" + categoria, null).body()).getAsJsonObject().get("nome").getAsString());
        assertEquals(1, JsonParser.parseString(requisitar("GET", "/api/categorias", null).body()).getAsJsonArray().size());
        assertEquals(204, requisitar("DELETE", "/api/categorias/" + categoria, null).statusCode());
    }
    @Test void validaEntradasConflitosERelacionamentos() throws Exception {
        String pessoa = "{\"nome\":\"Maria\",\"email\":\"maria@exemplo.com\",\"documento\":\"456\",\"telefone\":\"11\"}";
        long codigo = criar("/api/clientes", pessoa);
        assertEquals(409, requisitar("POST", "/api/clientes", pessoa).statusCode());
        assertEquals(400, requisitar("POST", "/api/clientes", "{}").statusCode());
        assertEquals(400, requisitar("POST", "/api/clientes", "{").statusCode());
        assertEquals(400, requisitar("POST", "/api/clientes", pessoa.replace("maria@exemplo.com", "invalido")).statusCode());
        assertEquals(400, requisitar("POST", "/api/clientes", pessoa.replace("\"nome\":\"Maria\"", "\"codigo\":1,\"nome\":\"Maria\"")).statusCode());
        assertEquals(400, requisitar("POST", "/api/clientes", pessoa.replace("\"nome\":\"Maria\"", "\"nome\":42")).statusCode());
        assertEquals(400, requisitar("GET", "/api/clientes/0", null).statusCode());
        assertEquals(400, requisitar("GET", "/api/clientes/99999999999999999999999999", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/clientes-errado", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/clientes/1/extra", null).statusCode());
        assertEquals(404, requisitar("PUT", "/api/clientes/9999", pessoa).statusCode());
        assertEquals(404, requisitar("DELETE", "/api/clientes/9999", null).statusCode());
        var metodo = requisitar("PATCH", "/api/clientes/" + codigo, "{}");
        assertEquals(405, metodo.statusCode());
        assertEquals("GET, PUT, DELETE", metodo.headers().firstValue("Allow").orElseThrow());
        assertEquals(400, requisitar("POST", "/api/tipos-servico", "{\"codigoCategoria\":999,\"nome\":\"Suporte\",\"descricao\":\"Teste\"}").statusCode());
        long categoria = criar("/api/categorias", "{\"nome\":\"TI\",\"descricao\":\"Tecnologia\"}");
        long tipo = criar("/api/tipos-servico", "{\"codigoCategoria\":" + categoria + ",\"nome\":\"Suporte\",\"descricao\":\"Teste\"}");
        assertEquals(409, requisitar("DELETE", "/api/categorias/" + categoria, null).statusCode());
        
        try (var conexao = banco.abrirConexao(); var stmt = conexao.prepareStatement("UPDATE tb_cad_tipo_servico SET codigo_categoria = 999999 WHERE codigo = ?")) {
            stmt.setLong(1, tipo);
            assertThrows(SQLException.class, () -> stmt.executeUpdate());
        }
    }
    @Test void mantemCadastroAoReiniciarServidorEBanco() throws Exception {
        long codigo = criar("/api/tecnicos", "{\"nome\":\"Pedro\",\"email\":\"pedro@exemplo.com\",\"especialidade\":\"Suporte\"}");
        servidor.stop(0);
        banco = new CarregarDados(pasta.resolve("teste.db"));
        banco.inicializar();
        iniciar();
        var resposta = requisitar("GET", "/api/tecnicos/" + codigo, null);
        assertEquals(200, resposta.statusCode());
        assertEquals("Pedro", JsonParser.parseString(resposta.body()).getAsJsonObject().get("nome").getAsString());
    }

    @Test void serveMenuPaginasEArquivosDoFrontendSemExporOutrosRecursos() throws Exception {
        String[] paginas = {"/", "/index.html", "/clientes.html", "/tecnicos.html", "/categorias.html", "/tipos-servico.html", "/ordens.html", "/ordem.html"};
        for (String pagina : paginas) {
            var resposta = requisitar("GET", pagina, null);
            assertEquals(200, resposta.statusCode(), pagina);
            assertTrue(resposta.headers().firstValue("Content-Type").orElseThrow().startsWith("text/html"));
            assertTrue(resposta.body().contains("SIGOS"));
        }
        for (String arquivo : new String[]{"/api.js", "/clientes.js", "/tecnicos.js", "/categorias.js", "/tipos-servico.js", "/ordens.js", "/ordem.js", "/style.css"}) {
            var resposta = requisitar("GET", arquivo, null);
            assertEquals(200, resposta.statusCode(), arquivo);
            String tipo = arquivo.endsWith(".css") ? "text/css" : "text/javascript";
            assertTrue(resposta.headers().firstValue("Content-Type").orElseThrow().startsWith(tipo));
        }
        assertEquals(404, requisitar("GET", "/schema.sql", null).statusCode());
        assertEquals(404, requisitar("GET", "/../schema.sql", null).statusCode());
        assertEquals(404, requisitar("GET", "/pagina-inexistente", null).statusCode());
        assertEquals(405, requisitar("POST", "/clientes.html", "{}").statusCode());
        var head = requisitar("HEAD", "/clientes.html", null);
        assertEquals(200, head.statusCode());
        assertEquals("", head.body());
        assertEquals(200, requisitar("GET", "/api/status", null).statusCode());
    }

    private long abrirOrdem() throws Exception {
        String unico = Long.toString(System.nanoTime());
        long cliente = criar("/api/clientes", "{\"nome\":\"Cliente\",\"email\":\"cliente@exemplo.com\",\"documento\":\"" + unico + "\",\"telefone\":\"11\"}");
        long categoria = criar("/api/categorias", "{\"nome\":\"TI " + unico + "\",\"descricao\":\"Tecnologia\"}");
        long tipo = criar("/api/tipos-servico", "{\"codigoCategoria\":" + categoria + ",\"nome\":\"Instalação\",\"descricao\":\"Software\"}");
        return criar("/api/ordens", "{\"codigoCliente\":" + cliente + ",\"codigoTipoServico\":" + tipo + ",\"titulo\":\"Instalar sistema\",\"descricao\":\"Preparar ambiente\",\"prioridade\":\"ALTA\"}");
    }

    private JsonObject consultar(String rota) throws Exception {
        var resposta = requisitar("GET", rota, null);
        assertEquals(200, resposta.statusCode(), resposta.body());
        return JsonParser.parseString(resposta.body()).getAsJsonObject();
    }

    private void habilitar(long codigoTecnico, long codigoOrdem) throws Exception {
        long tipo = consultar("/api/ordens/" + codigoOrdem).get("codigoTipoServico").getAsLong();
        assertEquals(204, requisitar("PUT", "/api/tecnicos/" + codigoTecnico + "/tipos-servico/" + tipo, null).statusCode());
    }

    private HttpResponse<String> status(String rota, String status) throws Exception {
        return requisitar("PATCH", rota + "/status", "{\"status\":\"" + status + "\"}");
    }

    @Test void ordemAtividadesETecnicosComConclusaoProtegida() throws Exception {
        long ordem = abrirOrdem();
        String rota = "/api/ordens/" + ordem;
        JsonObject inicial = consultar(rota);
        assertEquals("ABERTA", inicial.get("status").getAsString());
        String dataAbertura = inicial.get("dataAbertura").getAsString();
        java.time.LocalDateTime.parse(dataAbertura);
        JsonObject edicao = inicial.deepCopy();
        edicao.remove("codigo"); edicao.remove("status"); edicao.remove("dataAbertura");
        edicao.addProperty("titulo", "Novo título");
        assertEquals(200, requisitar("PUT", rota, edicao.toString()).statusCode());
        assertEquals(dataAbertura, consultar(rota).get("dataAbertura").getAsString());

        long atividade = criar("/api/atividades", "{\"codigoOrdem\":" + ordem + ",\"descricao\":\"Instalar\"}");
        String tarefa = "/api/atividades/" + atividade;
        var nested = requisitar("POST", rota + "/atividades", "{\"descricao\":\"Validar\"}");
        assertEquals(201, nested.statusCode());
        long segunda = JsonParser.parseString(nested.body()).getAsJsonObject().get("codigo").getAsLong();
        assertEquals("/api/atividades/" + segunda, nested.headers().firstValue("Location").orElseThrow());
        assertEquals(2, JsonParser.parseString(requisitar("GET", rota + "/atividades", null).body()).getAsJsonArray().size());

        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"Software\"}");
        long outro = criar("/api/tecnicos", "{\"nome\":\"Pedro\",\"email\":\"pedro@exemplo.com\",\"especialidade\":\"Redes\"}");
        habilitar(tecnico, ordem);
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + tecnico, null).statusCode());
        habilitar(tecnico, ordem);
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + tecnico, null).statusCode());
        habilitar(outro, ordem);
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + outro, null).statusCode());
        assertEquals(2, JsonParser.parseString(requisitar("GET", rota + "/tecnicos", null).body()).getAsJsonArray().size());
        long outraOrdem = abrirOrdem();
        habilitar(tecnico, outraOrdem);
        assertEquals(204, requisitar("PUT", "/api/ordens/" + outraOrdem + "/tecnicos/" + tecnico, null).statusCode());
        assertEquals(409, requisitar("DELETE", "/api/tecnicos/" + tecnico, null).statusCode());
        assertEquals(204, requisitar("DELETE", rota + "/tecnicos/" + outro, null).statusCode());

        assertEquals(409, status(rota, "CONCLUIDA").statusCode());
        assertEquals("ABERTA", consultar(rota).get("status").getAsString());
        assertEquals(200, status(rota, "EM_ANDAMENTO").statusCode());
        assertEquals(200, requisitar("PUT", tarefa, "{\"descricao\":\"Instalar versão nova\"}").statusCode());
        assertEquals(200, status(tarefa, "EM_EXECUCAO").statusCode());
        assertEquals(200, status(tarefa, "CONCLUIDA").statusCode());
        String conclusao = consultar(tarefa).get("dataConclusao").getAsString();
        assertEquals(200, status(tarefa, "CONCLUIDA").statusCode());
        assertEquals(conclusao, consultar(tarefa).get("dataConclusao").getAsString());
        assertEquals(200, status(tarefa, "PENDENTE").statusCode());
        assertTrue(consultar(tarefa).get("dataConclusao").isJsonNull());
        assertEquals(200, status(tarefa, "CONCLUIDA").statusCode());
        assertEquals(200, status("/api/atividades/" + segunda, "CONCLUIDA").statusCode());
        assertEquals(200, status(rota, "CONCLUIDA").statusCode());
        assertEquals(409, status(rota, "ABERTA").statusCode());
        assertEquals(409, requisitar("PUT", rota, edicao.toString()).statusCode());
        assertEquals(409, requisitar("POST", rota + "/atividades", "{\"descricao\":\"Extra\"}").statusCode());
        assertEquals(409, requisitar("PUT", tarefa, "{\"descricao\":\"Mudar\"}").statusCode());
        assertEquals(409, requisitar("DELETE", tarefa, null).statusCode());
        assertEquals(409, status(tarefa, "PENDENTE").statusCode());
        assertEquals(409, requisitar("DELETE", rota + "/tecnicos/" + tecnico, null).statusCode());
        assertEquals(409, requisitar("PUT", rota + "/tecnicos/" + outro, null).statusCode());
        assertEquals(dataAbertura, consultar(rota).get("dataAbertura").getAsString());
    }

    @Test void consultasValidacoesCancelamentoEExclusaoEmCascata() throws Exception {
        long ordem = abrirOrdem();
        String rota = "/api/ordens/" + ordem;
        JsonObject dadoFormulario = consultar(rota);
        long cliente = dadoFormulario.get("codigoCliente").getAsLong();
        long tipo = dadoFormulario.get("codigoTipoServico").getAsLong();
        assertEquals(1, JsonParser.parseString(requisitar("GET", "/api/ordens?codigoCliente=" + cliente, null).body()).getAsJsonArray().size());
        assertEquals(404, requisitar("GET", "/api/ordens?codigoCliente=9999", null).statusCode());
        assertEquals(400, requisitar("GET", "/api/ordens?codigoCliente=abc", null).statusCode());
        assertEquals(400, status(rota, "INVALIDO").statusCode());
        assertEquals(404, requisitar("GET", rota + "/extra", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/ordens-erradas", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/atividades/9999", null).statusCode());
        assertEquals(404, requisitar("POST", "/api/atividades", "{\"codigoOrdem\":9999,\"descricao\":\"X\"}").statusCode());
        assertEquals(400, requisitar("POST", "/api/ordens", "{\"codigoCliente\":9999,\"codigoTipoServico\":" + tipo + ",\"titulo\":\"X\",\"descricao\":\"X\",\"prioridade\":\"MEDIA\"}").statusCode());
        assertEquals(400, requisitar("POST", "/api/ordens", "{\"codigoCliente\":" + cliente + ",\"codigoTipoServico\":9999,\"titulo\":\"X\",\"descricao\":\"X\",\"prioridade\":\"MEDIA\"}").statusCode());
        assertEquals(400, requisitar("POST", rota + "/atividades", "{\"descricao\":\"\"}").statusCode());
        assertEquals(409, requisitar("DELETE", "/api/clientes/" + cliente, null).statusCode());
        assertEquals(409, requisitar("DELETE", "/api/tipos-servico/" + tipo, null).statusCode());
        long atividade = criar("/api/atividades", "{\"codigoOrdem\":" + ordem + ",\"descricao\":\"Remover\"}");
        assertEquals(204, requisitar("DELETE", "/api/atividades/" + atividade, null).statusCode());
        assertEquals(404, requisitar("DELETE", "/api/atividades/" + atividade, null).statusCode());
        atividade = criar("/api/atividades", "{\"codigoOrdem\":" + ordem + ",\"descricao\":\"Cancelar\"}");
        assertEquals(1, JsonParser.parseString(requisitar("GET", "/api/atividades?codigoOrdem=" + ordem, null).body()).getAsJsonArray().size());
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        assertEquals(404, requisitar("PUT", rota + "/tecnicos/9999", null).statusCode());
        assertEquals(404, requisitar("DELETE", rota + "/tecnicos/" + tecnico, null).statusCode());
        habilitar(tecnico, ordem);
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + tecnico, null).statusCode());
        assertEquals(200, status(rota, "CANCELADA").statusCode());
        assertEquals(409, status(rota, "EM_ANDAMENTO").statusCode());
        assertEquals(204, requisitar("DELETE", rota, null).statusCode());
        assertEquals(404, requisitar("GET", "/api/atividades/" + atividade, null).statusCode());
        assertEquals(404, requisitar("GET", rota + "/tecnicos", null).statusCode());
        assertEquals(200, requisitar("GET", "/api/tecnicos/" + tecnico, null).statusCode());
        try (var conexao = banco.abrirConexao(); var comando = conexao.createStatement(); var resultado = comando.executeQuery("SELECT count(*) FROM tb_ord_tecnico")) {
            assertTrue(resultado.next()); assertEquals(0, resultado.getInt(1));
        }
    }

    @Test void tecnicoTipoServicoCriaSegundoNnEProtegeAtribuicaoETrocaDeTipo() throws Exception {
        long ordem = abrirOrdem();
        String rota = "/api/ordens/" + ordem;
        JsonObject dadoFormulario = consultar(rota);
        long tipo = dadoFormulario.get("codigoTipoServico").getAsLong();
        long categoria = consultar("/api/tipos-servico/" + tipo).get("codigoCategoria").getAsLong();
        long outroTipo = criar("/api/tipos-servico", "{\"codigoCategoria\":" + categoria + ",\"nome\":\"Redes\",\"descricao\":\"Configuração\"}");
        long ana = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        long joao = criar("/api/tecnicos", "{\"nome\":\"João\",\"email\":\"joao@exemplo.com\",\"especialidade\":\"TI\"}");
        String anaTipos = "/api/tecnicos/" + ana + "/tipos-servico";
        String joaoTipos = "/api/tecnicos/" + joao + "/tipos-servico";
        assertEquals(0, JsonParser.parseString(requisitar("GET", anaTipos, null).body()).getAsJsonArray().size());
        assertEquals(409, requisitar("PUT", rota + "/tecnicos/" + ana, null).statusCode());
        assertEquals(204, requisitar("PUT", anaTipos + "/" + tipo, null).statusCode());
        assertEquals(204, requisitar("PUT", anaTipos + "/" + tipo, null).statusCode());
        assertEquals(204, requisitar("PUT", anaTipos + "/" + outroTipo, null).statusCode());
        assertEquals(204, requisitar("PUT", joaoTipos + "/" + tipo, null).statusCode());
        assertEquals(2, JsonParser.parseString(requisitar("GET", anaTipos, null).body()).getAsJsonArray().size());
        assertEquals(2, JsonParser.parseString(requisitar("GET", "/api/tecnicos?codigoTipoServico=" + tipo, null).body()).getAsJsonArray().size());
        assertEquals(1, JsonParser.parseString(requisitar("GET", "/api/tecnicos?codigoTipoServico=" + outroTipo, null).body()).getAsJsonArray().size());
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + ana, null).statusCode());
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + joao, null).statusCode());
        assertEquals(409, requisitar("DELETE", anaTipos + "/" + tipo, null).statusCode());

        dadoFormulario.remove("codigo"); dadoFormulario.remove("status"); dadoFormulario.remove("dataAbertura");
        dadoFormulario.addProperty("codigoTipoServico", outroTipo);
        assertEquals(409, requisitar("PUT", rota, dadoFormulario.toString()).statusCode());
        assertEquals(tipo, consultar(rota).get("codigoTipoServico").getAsLong());
        assertEquals(204, requisitar("PUT", joaoTipos + "/" + outroTipo, null).statusCode());
        assertEquals(200, requisitar("PUT", rota, dadoFormulario.toString()).statusCode());
        assertEquals(204, requisitar("DELETE", anaTipos + "/" + tipo, null).statusCode());
        assertEquals(409, requisitar("DELETE", anaTipos + "/" + outroTipo, null).statusCode());

        servidor.stop(0);
        banco = new CarregarDados(pasta.resolve("teste.db")); banco.inicializar(); iniciar();
        assertEquals(2, JsonParser.parseString(requisitar("GET", joaoTipos, null).body()).getAsJsonArray().size());
        assertEquals(200, status(rota, "CONCLUIDA").statusCode());
        assertEquals(204, requisitar("DELETE", anaTipos + "/" + outroTipo, null).statusCode());
        assertEquals(2, JsonParser.parseString(requisitar("GET", rota + "/tecnicos", null).body()).getAsJsonArray().size());
        assertEquals(404, requisitar("DELETE", anaTipos + "/" + outroTipo, null).statusCode());
    }

    @Test void validaReferenciasDasHabilitacoesELimpaVinculosDeCadastrosExcluidos() throws Exception {
        long ordem = abrirOrdem();
        long tipo = consultar("/api/ordens/" + ordem).get("codigoTipoServico").getAsLong();
        long categoria = consultar("/api/tipos-servico/" + tipo).get("codigoCategoria").getAsLong();
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        String rota = "/api/tecnicos/" + tecnico + "/tipos-servico";
        assertEquals(404, requisitar("PUT", "/api/tecnicos/9999/tipos-servico/" + tipo, null).statusCode());
        assertEquals(404, requisitar("PUT", rota + "/9999", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/tecnicos/9999/tipos-servico", null).statusCode());
        assertEquals(404, requisitar("GET", "/api/tecnicos?codigoTipoServico=9999", null).statusCode());
        assertEquals(400, requisitar("PUT", rota + "/abc", null).statusCode());
        assertEquals(400, requisitar("GET", "/api/tecnicos?codigoTipoServico=0", null).statusCode());
        assertEquals(405, requisitar("POST", rota + "/" + tipo, "{}").statusCode());
        assertEquals(404, requisitar("GET", rota + "/" + tipo + "/extra", null).statusCode());
        long livre = criar("/api/tipos-servico", "{\"codigoCategoria\":" + categoria + ",\"nome\":\"Livre\",\"descricao\":\"Teste\"}");
        assertEquals(204, requisitar("PUT", rota + "/" + livre, null).statusCode());
        assertEquals(204, requisitar("DELETE", "/api/tipos-servico/" + livre, null).statusCode());
        assertEquals(0, JsonParser.parseString(requisitar("GET", rota, null).body()).getAsJsonArray().size());
        assertEquals(204, requisitar("PUT", rota + "/" + tipo, null).statusCode());
        assertEquals(204, requisitar("DELETE", "/api/tecnicos/" + tecnico, null).statusCode());
        assertEquals(0, JsonParser.parseString(requisitar("GET", "/api/tecnicos?codigoTipoServico=" + tipo, null).body()).getAsJsonArray().size());
    }

    @Test void inicializacaoAdicionaTabelaSemApagarBancoAnteriorOuInventarHabilitacoes() throws Exception {
        long ordem = abrirOrdem();
        long tipo = consultar("/api/ordens/" + ordem).get("codigoTipoServico").getAsLong();
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        try (var conexao = banco.abrirConexao(); var comando = conexao.createStatement()) {
            
            comando.execute("DROP TABLE tb_cad_tecnico_tipo_servico");
            try (var inserir = conexao.prepareStatement("INSERT INTO tb_ord_tecnico VALUES (?, ?)")) {
                inserir.setLong(1, ordem); inserir.setLong(2, tecnico); inserir.executeUpdate();
            }
        }
        banco.inicializar(); banco.inicializar();
        assertEquals(tipo, consultar("/api/ordens/" + ordem).get("codigoTipoServico").getAsLong());
        assertEquals(1, JsonParser.parseString(requisitar("GET", "/api/ordens/" + ordem + "/tecnicos", null).body()).getAsJsonArray().size());
        assertEquals(0, JsonParser.parseString(requisitar("GET", "/api/tecnicos/" + tecnico + "/tipos-servico", null).body()).getAsJsonArray().size());
        assertEquals(409, requisitar("PUT", "/api/ordens/" + ordem + "/tecnicos/" + tecnico, null).statusCode());
        habilitar(tecnico, ordem);
        assertEquals(204, requisitar("PUT", "/api/ordens/" + ordem + "/tecnicos/" + tecnico, null).statusCode());
        try (var conexao = banco.abrirConexao(); var comando = conexao.createStatement(); var resultado = comando.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'tb_%'")) {
            assertTrue(resultado.next()); assertEquals(8, resultado.getInt(1));
        }
    }

    @Test void removerHabilitacaoEAtribuirTecnicoAoMesmoTempoMantemConsistencia() throws Exception {
        long ordem = abrirOrdem();
        long tipo = consultar("/api/ordens/" + ordem).get("codigoTipoServico").getAsLong();
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        habilitar(tecnico, ordem);
        var inicio = new java.util.concurrent.CountDownLatch(1);
        try (var tarefas = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var atribuir = tarefas.submit(() -> {
                inicio.await();
                try { new OrdemServicoService(banco).associarTecnico(ordem, tecnico); return true; }
                catch (IllegalStateException e) { return false; }
            });
            var remover = tarefas.submit(() -> {
                inicio.await();
                try { new TecnicoTipoServicoService(banco).remover(tecnico, tipo); return true; }
                catch (IllegalStateException e) { return false; }
            });
            inicio.countDown();
            boolean atribuido = atribuir.get();
            assertNotEquals(atribuido, remover.get());
            assertEquals(atribuido ? 1 : 0, JsonParser.parseString(requisitar("GET", "/api/ordens/" + ordem + "/tecnicos", null).body()).getAsJsonArray().size());
            assertEquals(atribuido ? 1 : 0, JsonParser.parseString(requisitar("GET", "/api/tecnicos/" + tecnico + "/tipos-servico", null).body()).getAsJsonArray().size());
        }
    }

    @Test void cargaInicialInsereCadastroEVinculoSemOrdemENaoRepete() throws Exception {
        banco.carregarDadoInicial();
        try (var conexao = banco.abrirConexao(); var comando = conexao.createStatement()) {
            String[] tabela = {"tb_cad_tecnico", "tb_cad_cliente", "tb_cad_categoria_servico", "tb_cad_tipo_servico", "tb_cad_tecnico_tipo_servico", "tb_ord_servico", "tb_ord_atividade", "tb_ord_tecnico"};
            int[] quantidade = {10, 10, 5, 10, 22, 0, 0, 0};
            for (int i = 0; i < tabela.length; i++) {
                try (var resultado = comando.executeQuery("SELECT count(*) FROM " + tabela[i])) {
                    assertTrue(resultado.next()); assertEquals(quantidade[i], resultado.getInt(1), tabela[i]);
                }
            }
            comando.executeUpdate("UPDATE tb_cad_tecnico SET nome = 'Nome editado' WHERE email = 'tecnico1@example.com'");
            comando.executeUpdate("DELETE FROM tb_cad_cliente WHERE documento = 'DEMO-CLIENTE-001'");
        }
        banco.carregarDadoInicial();
        try (var conexao = banco.abrirConexao(); var comando = conexao.createStatement()) {
            try (var resultado = comando.executeQuery("SELECT nome FROM tb_cad_tecnico WHERE email = 'tecnico1@example.com'")) {
                assertTrue(resultado.next()); assertEquals("Nome editado", resultado.getString(1));
            }
            try (var resultado = comando.executeQuery("SELECT count(*) FROM tb_cad_cliente")) {
                assertTrue(resultado.next()); assertEquals(9, resultado.getInt(1));
            }
            try (var resultado = comando.executeQuery("PRAGMA foreign_key_check")) { assertFalse(resultado.next()); }
        }
    }

    @Test void cargaInicialPreservaOrdemAnteriorERegistraVinculoComCodigoExistente() throws Exception {
        long ordem = abrirOrdem();
        JsonObject anterior = consultar("/api/ordens/" + ordem);
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Técnico anterior\",\"email\":\"tecnico1@example.com\",\"especialidade\":\"Perfil anterior\"}");
        banco.carregarDadoInicial();
        assertEquals(anterior, consultar("/api/ordens/" + ordem));
        assertEquals("Técnico anterior", consultar("/api/tecnicos/" + tecnico).get("nome").getAsString());
        assertEquals(2, JsonParser.parseString(requisitar("GET", "/api/tecnicos/" + tecnico + "/tipos-servico", null).body()).getAsJsonArray().size());
    }

    @Test void duasOperacoesSimultaneasNaoCriamPendenciaEmOrdemConcluida() throws Exception {
        long codigo = abrirOrdem();
        var inicio = new java.util.concurrent.CountDownLatch(1);
        try (var tarefas = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var concluir = tarefas.submit(() -> {
                inicio.await();
                try {
                    new OrdemServicoService(banco).alterarStatus(codigo, br.com.sigos.model.StatusOrdem.CONCLUIDA);
                    return true;
                } catch (IllegalStateException e) { return false; }
            });
            var adicionar = tarefas.submit(() -> {
                inicio.await();
                try {
                    new AtividadeService(banco).cadastrar(new br.com.sigos.model.Atividade(null, codigo, "Tarefa concorrente"));
                    return true;
                } catch (IllegalStateException e) { return false; }
            });
            inicio.countDown();
            boolean concluida = concluir.get();
            boolean criada = adicionar.get();
            assertNotEquals(concluida, criada, "Apenas uma operação pode ser aceita.");
            var lista = JsonParser.parseString(requisitar("GET", "/api/ordens/" + codigo + "/atividades", null).body()).getAsJsonArray();
            assertEquals(concluida ? "CONCLUIDA" : "ABERTA", consultar("/api/ordens/" + codigo).get("status").getAsString());
            assertEquals(concluida ? 0 : 1, lista.size());
        }
    }

    @Test void recuperaDatasEstadosEVinculosDepoisDeReiniciar() throws Exception {
        long ordem = abrirOrdem();
        String rota = "/api/ordens/" + ordem;
        long atividade = criar("/api/atividades", "{\"codigoOrdem\":" + ordem + ",\"descricao\":\"Teste\"}");
        long tecnico = criar("/api/tecnicos", "{\"nome\":\"Ana\",\"email\":\"ana@exemplo.com\",\"especialidade\":\"TI\"}");
        habilitar(tecnico, ordem);
        assertEquals(204, requisitar("PUT", rota + "/tecnicos/" + tecnico, null).statusCode());
        status("/api/atividades/" + atividade, "CONCLUIDA");
        status(rota, "CONCLUIDA");
        JsonObject antes = consultar(rota);
        JsonObject tarefa = consultar("/api/atividades/" + atividade);
        servidor.stop(0);
        banco = new CarregarDados(pasta.resolve("teste.db"));
        banco.inicializar(); iniciar();
        assertEquals(antes, consultar(rota));
        assertEquals(tarefa, consultar("/api/atividades/" + atividade));
        assertEquals(1, JsonParser.parseString(requisitar("GET", rota + "/tecnicos", null).body()).getAsJsonArray().size());
        long semAtividade = abrirOrdem();
        assertEquals(200, status("/api/ordens/" + semAtividade, "CONCLUIDA").statusCode());
    }
}
