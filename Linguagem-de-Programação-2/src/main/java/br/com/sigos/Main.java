package br.com.sigos;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.controller.OrdemServicoController;
import br.com.sigos.controller.AtividadeController;
import br.com.sigos.service.OrdemServicoService;
import br.com.sigos.service.AtividadeService;
import br.com.sigos.controller.PaginaController;
import br.com.sigos.controller.StatusController;
import br.com.sigos.controller.ClienteController;
import br.com.sigos.controller.TecnicoController;
import br.com.sigos.controller.CategoriaServicoController;
import br.com.sigos.controller.TipoServicoController;
import br.com.sigos.repository.ClienteRepository;
import br.com.sigos.repository.TecnicoRepository;
import br.com.sigos.repository.CategoriaServicoRepository;
import br.com.sigos.repository.TipoServicoRepository;
import br.com.sigos.service.ClienteService;
import br.com.sigos.service.TecnicoService;
import br.com.sigos.service.TecnicoTipoServicoService;
import br.com.sigos.service.CategoriaServicoService;
import br.com.sigos.service.TipoServicoService;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public final class Main {

    private Main() {}

    public static void main(String[] argumento) throws Exception {
        int porta = Integer.parseInt(System.getProperty("sigos.porta", "8080"));
        var banco = new CarregarDados(Path.of(System.getProperty("sigos.banco", "data/sigos.db")));
        banco.inicializar();
        banco.carregarDadoInicial();
        var servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
        servidor.createContext("/", new PaginaController());
        servidor.createContext("/api/status", new StatusController());
        ClienteRepository clienteRepository = new ClienteRepository(banco);
        ClienteService clienteService = new ClienteService(clienteRepository);
        servidor.createContext("/api/clientes", new ClienteController(clienteService));

        TecnicoRepository tecnicoRepository = new TecnicoRepository(banco);
        TecnicoService tecnicoService = new TecnicoService(tecnicoRepository);
        servidor.createContext("/api/tecnicos", new TecnicoController(tecnicoService, new TecnicoTipoServicoService(banco)));

        CategoriaServicoRepository categoriaRepository = new CategoriaServicoRepository(banco);
        CategoriaServicoService categoriaService = new CategoriaServicoService(categoriaRepository);
        servidor.createContext("/api/categorias", new CategoriaServicoController(categoriaService));

        TipoServicoRepository tipoRepository = new TipoServicoRepository(banco);
        TipoServicoService tipoService = new TipoServicoService(tipoRepository, categoriaRepository);
        servidor.createContext("/api/tipos-servico", new TipoServicoController(tipoService));
        AtividadeService atividadeService = new AtividadeService(banco);
        OrdemServicoService ordemService = new OrdemServicoService(banco);
        servidor.createContext("/api/ordens", new OrdemServicoController(ordemService, atividadeService));
        servidor.createContext("/api/atividades", new AtividadeController(atividadeService));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> servidor.stop(1)));
        servidor.start();
        System.out.println("SIGOS iniciado. Abra no navegador: http://localhost:" + porta + "/");
    }
}
