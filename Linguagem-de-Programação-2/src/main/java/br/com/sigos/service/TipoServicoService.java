package br.com.sigos.service;

import br.com.sigos.model.TipoServico;
import br.com.sigos.model.CategoriaServico;
import br.com.sigos.repository.Repositorio;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class TipoServicoService {
    private final Repositorio<TipoServico> repositorio;
    private final Repositorio<CategoriaServico> categoriaServicoRepository;


    public TipoServicoService(Repositorio<TipoServico> repositorio, Repositorio<CategoriaServico> categoriaServicoRepository) {
        this.repositorio = repositorio;
        this.categoriaServicoRepository = categoriaServicoRepository;
    }


    public TipoServico cadastrar(TipoServico entidade) throws SQLException {
        validarCategoria(entidade.getCodigoCategoria());
        return repositorio.inserir(entidade);
    }


    public List<TipoServico> listar() throws SQLException {
        return repositorio.listar();
    }


    public TipoServico buscarPorCodigo(long codigo) throws SQLException {
        validarCodigo(codigo);
        var entidade = repositorio.buscarPorCodigo(codigo);
        if (entidade.isEmpty()) {
            throw new NoSuchElementException("Registro não encontrado.");
        }
        return entidade.get();
    }


    public TipoServico atualizar(long codigo, TipoServico entidade) throws SQLException {
        buscarPorCodigo(codigo);
        validarCategoria(entidade.getCodigoCategoria());
        boolean atualizado = repositorio.atualizar(codigo, entidade);
        if (!atualizado) {
            throw new NoSuchElementException("Registro não encontrado.");
        }
        return buscarPorCodigo(codigo);
    }


    public void excluir(long codigo) throws SQLException {
        validarCodigo(codigo);
        boolean excluido = repositorio.excluir(codigo);
        if (!excluido) {
            throw new NoSuchElementException("Registro não encontrado.");
        }
    }


    private void validarCodigo(long codigo) {
        if (codigo <= 0) {
            throw new IllegalArgumentException("Código deve ser positivo.");
        }
    }


    private void validarCategoria(long codigoCategoria) throws SQLException {
        if (categoriaServicoRepository.buscarPorCodigo(codigoCategoria).isEmpty()) {
            throw new IllegalArgumentException("Categoria informada não existe.");
        }
    }
}
