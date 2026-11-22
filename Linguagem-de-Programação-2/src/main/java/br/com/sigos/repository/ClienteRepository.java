package br.com.sigos.repository;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.model.Cliente;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteRepository implements Repositorio<Cliente> {
    private final CarregarDados banco;


    public ClienteRepository(CarregarDados banco) {
        this.banco = banco;
    }

    @Override

    public Cliente inserir(Cliente entidade) throws SQLException {
        
        String sql = "INSERT INTO tb_cad_cliente (nome, email, documento, telefone) VALUES (?, ?, ?, ?) RETURNING *";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getEmail());
            comando.setString(3, entidade.getDocumento());
            comando.setString(4, entidade.getTelefone());
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                throw new SQLException("Não foi possível obter o cadastro inserido.");
            }
        }
    }

    @Override

    public Optional<Cliente> buscarPorCodigo(long codigo) throws SQLException {
        String sql = "SELECT * FROM tb_cad_cliente WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigo);
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return Optional.of(mapear(resultado));
                }
                return Optional.empty();
            }
        }
    }

    @Override

    public List<Cliente> listar() throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cad_cliente ORDER BY codigo";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql);
             ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                lista.add(mapear(resultado));
            }
        }
        return lista;
    }

    @Override

    public boolean atualizar(long codigo, Cliente entidade) throws SQLException {
        String sql = "UPDATE tb_cad_cliente SET nome = ?, email = ?, documento = ?, telefone = ? WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getEmail());
            comando.setString(3, entidade.getDocumento());
            comando.setString(4, entidade.getTelefone());
            comando.setLong(5, codigo);
            return comando.executeUpdate() > 0;
        }
    }

    @Override

    public boolean excluir(long codigo) throws SQLException {
        String sql = "DELETE FROM tb_cad_cliente WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigo);
            return comando.executeUpdate() > 0;
        }
    }


    private Cliente mapear(ResultSet resultado) throws SQLException {
        return new Cliente(
                resultado.getLong("codigo"),
                resultado.getString("nome"),
                resultado.getString("email"),
                resultado.getString("documento"),
                resultado.getString("telefone"));
    }
}
