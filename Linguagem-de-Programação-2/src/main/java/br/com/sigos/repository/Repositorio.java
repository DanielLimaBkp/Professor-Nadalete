package br.com.sigos.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface Repositorio<T> {
    T inserir(T entidade) throws SQLException;
    Optional<T> buscarPorCodigo(long codigo) throws SQLException;
    List<T> listar() throws SQLException;
    boolean atualizar(long codigo, T entidade) throws SQLException;
    boolean excluir(long codigo) throws SQLException;
}
