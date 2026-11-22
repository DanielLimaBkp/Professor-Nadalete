package br.com.sigos.service;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.config.Transaction;
import br.com.sigos.model.Tecnico;
import br.com.sigos.model.TipoServico;
import br.com.sigos.repository.TecnicoRepository;
import br.com.sigos.repository.TipoServicoRepository;
import br.com.sigos.repository.TecnicoTipoServicoRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class TecnicoTipoServicoService {
    private final CarregarDados banco;
    private final TecnicoTipoServicoRepository tecnicoTipoServicoRepository = new TecnicoTipoServicoRepository();
    private final TecnicoRepository tecnicoRepository;
    private final TipoServicoRepository tipoServicoRepository;


    public TecnicoTipoServicoService(CarregarDados banco) {
        this.banco = banco;
        tecnicoRepository = new TecnicoRepository(banco);
        tipoServicoRepository = new TipoServicoRepository(banco);
    }

    public List<TipoServico> listarTipoServicoHabilitado(long codigoTecnico) throws SQLException {
        validarTecnico(codigoTecnico);
        try (var conexao = banco.abrirConexao()) { return tecnicoTipoServicoRepository.listarTipoServicoHabilitado(conexao, codigoTecnico); }
    }

    public List<Tecnico> listarTecnicoHabilitado(long codigoTipoServico) throws SQLException {
        validarTipo(codigoTipoServico);
        try (var conexao = banco.abrirConexao()) { return tecnicoTipoServicoRepository.listarTecnicoHabilitado(conexao, codigoTipoServico); }
    }

    public void associar(long codigoTecnico, long codigoTipoServico) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            validarTecnico(codigoTecnico);
            validarTipo(codigoTipoServico);
            tecnicoTipoServicoRepository.associar(transacao.getConexao(), codigoTecnico, codigoTipoServico);
            transacao.confirmar();
        }
    }

    public void remover(long codigoTecnico, long codigoTipoServico) throws SQLException {
        try (var transacao = new Transaction(banco)) {
            validarTecnico(codigoTecnico);
            validarTipo(codigoTipoServico);
            if (tecnicoTipoServicoRepository.usadaEmOrdemAberta(transacao.getConexao(), codigoTecnico, codigoTipoServico))
                throw new IllegalStateException("Habilitação em uso por uma ordem aberta. Remova o técnico da ordem ou encerre o atendimento primeiro.");
            if (!tecnicoTipoServicoRepository.remover(transacao.getConexao(), codigoTecnico, codigoTipoServico))
                throw new NoSuchElementException("Habilitação não encontrada.");
            transacao.confirmar();
        }
    }

    private void validarTecnico(long codigo) throws SQLException {
        if (codigo <= 0) throw new IllegalArgumentException("Código deve ser positivo.");
        if (tecnicoRepository.buscarPorCodigo(codigo).isEmpty()) throw new NoSuchElementException("Técnico não encontrado.");
    }

    private void validarTipo(long codigo) throws SQLException {
        if (codigo <= 0) throw new IllegalArgumentException("Código deve ser positivo.");
        if (tipoServicoRepository.buscarPorCodigo(codigo).isEmpty()) throw new NoSuchElementException("Tipo de serviço não encontrado.");
    }
}
