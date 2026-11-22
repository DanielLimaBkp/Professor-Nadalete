package br.com.sigos.config;

import java.sql.Connection;
import java.sql.SQLException;

public class Transaction implements AutoCloseable {
    private final Connection conexao;

    private boolean confirmada;


    public Transaction(CarregarDados banco) throws SQLException {
        conexao = banco.abrirConexao();
        try (var comando = conexao.createStatement()) {

            comando.execute("BEGIN IMMEDIATE");
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
    }

    public Connection getConexao() { return conexao; }

    public void confirmar() throws SQLException {
        try (var comando = conexao.createStatement()) { comando.execute("COMMIT"); }
        confirmada = true;
    }
    @Override public void close() throws SQLException {
        try {
            if (!confirmada) {
                try (var comando = conexao.createStatement()) { comando.execute("ROLLBACK"); }
            }
        } finally { conexao.close(); }
    }
}
