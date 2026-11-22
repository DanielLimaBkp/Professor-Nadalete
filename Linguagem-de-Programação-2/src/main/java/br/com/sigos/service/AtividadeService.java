package br.com.sigos.service;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.config.Transaction;
import br.com.sigos.model.*;
import br.com.sigos.repository.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class AtividadeService {
    private final CarregarDados banco;
    private final AtividadeRepository atividadeRepository = new AtividadeRepository();
    private final OrdemServicoRepository ordemServicoRepository = new OrdemServicoRepository();


    public AtividadeService(CarregarDados banco) { this.banco = banco; }


    public Atividade cadastrar(Atividade atividade) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            buscarOrdem(transacao.getConexao(), atividade.getCodigoOrdem()).validarEdicao();
            Atividade salva = atividadeRepository.inserir(transacao.getConexao(), atividade);
            transacao.confirmar();
            return salva;
        }
    }

    public Atividade buscarPorCodigo(long codigo) throws SQLException {
        try (var conexao = banco.abrirConexao()) { return buscar(conexao, codigo); }
    }

    public List<Atividade> listar(Long codigoOrdem) throws SQLException {
        try (var conexao = banco.abrirConexao()) {
            if (codigoOrdem != null) buscarOrdem(conexao, codigoOrdem);
            return atividadeRepository.listar(conexao, codigoOrdem);
        }
    }

    public Atividade atualizar(long codigo, String descricao) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            var conexao = transacao.getConexao();
            Atividade atual = buscar(conexao, codigo);
            buscarOrdem(conexao, atual.getCodigoOrdem()).validarEdicao();
            Atividade alterada = new Atividade(codigo, atual.getCodigoOrdem(), descricao, atual.getStatus(), atual.getDataConclusao());
            atividadeRepository.atualizar(conexao, alterada);
            transacao.confirmar();
            return alterada;
        }
    }

    public Atividade alterarStatus(long codigo, StatusAtividade status) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            var conexao = transacao.getConexao();
            Atividade atividade = buscar(conexao, codigo);
            buscarOrdem(conexao, atividade.getCodigoOrdem()).validarEdicao();
            atividade.alterarStatus(status);
            atividadeRepository.atualizar(conexao, atividade);
            transacao.confirmar();
            return atividade;
        }
    }

    public void excluir(long codigo) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            var conexao = transacao.getConexao();
            Atividade atividade = buscar(conexao, codigo);
            buscarOrdem(conexao, atividade.getCodigoOrdem()).validarEdicao();
            atividadeRepository.excluir(conexao, codigo);
            transacao.confirmar();
        }
    }

    private Atividade buscar(Connection conexao, long codigo) throws SQLException {
        if (codigo <= 0) throw new IllegalArgumentException("Código deve ser positivo.");
        Atividade atividade = atividadeRepository.buscarPorCodigo(conexao, codigo);
        if (atividade == null) throw new NoSuchElementException("Atividade não encontrada.");
        return atividade;
    }

    private OrdemServico buscarOrdem(Connection conexao, long codigo) throws SQLException {
        OrdemServico ordem = ordemServicoRepository.buscarPorCodigo(conexao, codigo);
        if (ordem == null) throw new NoSuchElementException("Ordem não encontrada.");
        return ordem;
    }
}
