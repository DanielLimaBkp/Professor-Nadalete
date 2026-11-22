package br.com.sigos.service;

import br.com.sigos.model.CategoriaServico;
import br.com.sigos.repository.Repositorio;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class CategoriaServicoService {
    private final Repositorio<CategoriaServico> repositorio;


    public CategoriaServicoService(Repositorio<CategoriaServico> repositorio) {
        this.repositorio = repositorio;
    }


    public CategoriaServico cadastrar(CategoriaServico entidade) throws SQLException {
        return repositorio.inserir(entidade);
    }


    public List<CategoriaServico> listar() throws SQLException {
        return repositorio.listar();
    }


    public CategoriaServico buscarPorCodigo(long codigo) throws SQLException {
        validarCodigo(codigo);
        var entidade = repositorio.buscarPorCodigo(codigo);
        if (entidade.isEmpty()) {
            throw new NoSuchElementException("Registro não encontrado.");
        }
        return entidade.get();
    }


    public CategoriaServico atualizar(long codigo, CategoriaServico entidade) throws SQLException {
        buscarPorCodigo(codigo);
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
}
