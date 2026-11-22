package br.com.sigos.service;

import br.com.sigos.model.Tecnico;
import br.com.sigos.repository.Repositorio;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;

public class TecnicoService {
    private final Repositorio<Tecnico> repositorio;


    public TecnicoService(Repositorio<Tecnico> repositorio) {
        this.repositorio = repositorio;
    }


    public Tecnico cadastrar(Tecnico entidade) throws SQLException {
        return repositorio.inserir(entidade);
    }


    public List<Tecnico> listar() throws SQLException {
        return repositorio.listar();
    }


    public Tecnico buscarPorCodigo(long codigo) throws SQLException {
        validarCodigo(codigo);
        var entidade = repositorio.buscarPorCodigo(codigo);
        if (entidade.isEmpty()) {
            throw new NoSuchElementException("Registro não encontrado.");
        }
        return entidade.get();
    }


    public Tecnico atualizar(long codigo, Tecnico entidade) throws SQLException {
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
