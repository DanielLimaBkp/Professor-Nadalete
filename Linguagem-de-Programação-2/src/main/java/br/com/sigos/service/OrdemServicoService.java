package br.com.sigos.service;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.config.Transaction;
import br.com.sigos.model.*;
import br.com.sigos.repository.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class OrdemServicoService {
    private final CarregarDados banco;
    private final OrdemServicoRepository ordemServicoRepository = new OrdemServicoRepository();
    private final AtividadeRepository atividadeRepository = new AtividadeRepository();
    private final TecnicoTipoServicoRepository tecnicoTipoServicoRepository = new TecnicoTipoServicoRepository();
    private final ClienteRepository clienteRepository;
    private final TipoServicoRepository tipoServicoRepository;
    private final TecnicoRepository tecnicoRepository;


    public OrdemServicoService(CarregarDados banco) {
        this.banco = banco;
        clienteRepository = new ClienteRepository(banco);
        tipoServicoRepository = new TipoServicoRepository(banco);
        tecnicoRepository = new TecnicoRepository(banco);
    }

    public OrdemServico cadastrar(OrdemServico ordem) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            validarReferencias(ordem);
            OrdemServico salva = ordemServicoRepository.inserir(transacao.getConexao(), ordem);
            transacao.confirmar();
            return salva;
        }
    }

    public List<OrdemServico> listar(Long codigoCliente) throws SQLException {
        if (codigoCliente != null && clienteRepository.buscarPorCodigo(codigoCliente).isEmpty())
            throw new NoSuchElementException("Cliente não encontrado.");
        try (var conexao = banco.abrirConexao()) { return ordemServicoRepository.listar(conexao, codigoCliente); }
    }

    public OrdemServico buscarPorCodigo(long codigo) throws SQLException {
        try (var conexao = banco.abrirConexao()) { return buscar(conexao, codigo); }
    }

    private OrdemServico buscar(Connection conexao, long codigo) throws SQLException {
        if (codigo <= 0) throw new IllegalArgumentException("Código deve ser positivo.");
        OrdemServico ordem = ordemServicoRepository.buscarPorCodigo(conexao, codigo);
        if (ordem == null) throw new NoSuchElementException("Ordem não encontrada.");
        return ordem;
    }

    public OrdemServico atualizar(long codigo, OrdemServico dadoFormulario) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            OrdemServico atual = buscar(transacao.getConexao(), codigo);
            atual.validarEdicao();
            validarReferencias(dadoFormulario);
            for (Tecnico tecnico : ordemServicoRepository.listarTecnicoResponsavel(transacao.getConexao(), codigo)) {
                if (!tecnicoTipoServicoRepository.estaHabilitado(transacao.getConexao(), tecnico.getCodigo(), dadoFormulario.getCodigoTipoServico()))
                    throw new IllegalStateException("Há técnico atribuído sem habilitação para este tipo de serviço. Ajuste os responsáveis ou as habilitações primeiro.");
            }
            OrdemServico alterada = new OrdemServico(codigo, dadoFormulario.getCodigoCliente(), dadoFormulario.getCodigoTipoServico(),
                    dadoFormulario.getTitulo(), dadoFormulario.getDescricao(), dadoFormulario.getPrioridade(), atual.getStatus(), atual.getDataAbertura());
            ordemServicoRepository.atualizar(transacao.getConexao(), alterada);
            transacao.confirmar();
            return alterada;
        }
    }

    public OrdemServico alterarStatus(long codigo, StatusOrdem status) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            var conexao = transacao.getConexao();
            OrdemServico ordem = buscar(conexao, codigo);
            ordem.alterarStatus(status, atividadeRepository.listar(conexao, codigo));
            ordemServicoRepository.atualizar(conexao, ordem);
            transacao.confirmar();
            return ordem;
        }
    }

    public void excluir(long codigo) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            buscar(transacao.getConexao(), codigo);
            ordemServicoRepository.excluir(transacao.getConexao(), codigo);
            transacao.confirmar();
        }
    }

    public List<Tecnico> listarTecnicoResponsavel(long codigo) throws SQLException {
        try (var conexao = banco.abrirConexao()) {
            buscar(conexao, codigo);
            return ordemServicoRepository.listarTecnicoResponsavel(conexao, codigo);
        }
    }

    public void associarTecnico(long codigoOrdem, long codigoTecnico) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            OrdemServico ordem = buscar(transacao.getConexao(), codigoOrdem);
            ordem.validarEdicao();
            if (tecnicoRepository.buscarPorCodigo(codigoTecnico).isEmpty())
                throw new NoSuchElementException("Técnico não encontrado.");
            if (!tecnicoTipoServicoRepository.estaHabilitado(transacao.getConexao(), codigoTecnico, ordem.getCodigoTipoServico()))
                throw new IllegalStateException("Técnico não habilitado para o tipo de serviço desta ordem.");
            ordemServicoRepository.associarTecnico(transacao.getConexao(), codigoOrdem, codigoTecnico);
            transacao.confirmar();
        }
    }

    public void removerTecnico(long codigoOrdem, long codigoTecnico) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            buscar(transacao.getConexao(), codigoOrdem).validarEdicao();
            if (!ordemServicoRepository.removerTecnico(transacao.getConexao(), codigoOrdem, codigoTecnico))
                throw new NoSuchElementException("Vínculo não encontrado.");
            transacao.confirmar();
        }
    }

    private void validarReferencias(OrdemServico ordem) throws SQLException {
        if (clienteRepository.buscarPorCodigo(ordem.getCodigoCliente()).isEmpty())
            throw new IllegalArgumentException("Cliente informado não existe.");
        if (tipoServicoRepository.buscarPorCodigo(ordem.getCodigoTipoServico()).isEmpty())
            throw new IllegalArgumentException("Tipo de serviço informado não existe.");
    }
}
