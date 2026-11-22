package br.com.sigos.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class CarregarDados {
    private final String url;

    public CarregarDados(Path arquivo) throws IOException {
        Path absoluto = arquivo.toAbsolutePath();
        Files.createDirectories(absoluto.getParent());
        url = "jdbc:sqlite:" + absoluto;
    }

    public Connection abrirConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection(url);
        try (var stmt = conexao.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
        return conexao;
    }

    public void inicializar() throws IOException, SQLException {
        try (var recurso = CarregarDados.class.getResourceAsStream("/schema.sql")) {
            if (recurso == null) throw new IOException("schema.sql não encontrado.");
            String sql = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
            try (var conexao = abrirConexao()) {
                conexao.setAutoCommit(false);
                try (var stmt = conexao.createStatement()) {
                    for (String comando : sql.split(";")) {
                        if (!comando.isBlank()) stmt.execute(comando);
                    }
                    conexao.commit();
                } catch (SQLException e) {
                    conexao.rollback();
                    throw e;
                }
            }
        }
    }


    public void carregarDadoInicial() throws IOException, SQLException {
        try (var transacao = new Transaction(this)) {
            var conexao = transacao.getConexao();
            boolean carregado;
            try (var comando = conexao.createStatement(); var resultado = comando.executeQuery("PRAGMA user_version")) {
                carregado = resultado.next() && resultado.getInt(1) >= 1;
            }
            if (carregado) {
                transacao.confirmar();
                return;
            }
            try (var recurso = CarregarDados.class.getResourceAsStream("/dado-inicial.sql")) {
                if (recurso == null) throw new IOException("dado-inicial.sql não encontrado.");
                String sql = new String(recurso.readAllBytes(), StandardCharsets.UTF_8);
                try (var comando = conexao.createStatement()) {
                    for (String instrucao : sql.split(";")) {
                        if (!instrucao.isBlank()) comando.execute(instrucao);
                    }
                    comando.execute("PRAGMA user_version = 1");
                }
            }
            transacao.confirmar();
        }
    }
}
