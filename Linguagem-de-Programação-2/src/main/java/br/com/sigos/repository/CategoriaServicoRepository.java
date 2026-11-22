package br.com.sigos.repository;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.model.CategoriaServico;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaServicoRepository implements Repositorio<CategoriaServico> {
    private final CarregarDados banco;


    public CategoriaServicoRepository(CarregarDados banco) {
        this.banco = banco;
    }

    @Override

    public CategoriaServico inserir(CategoriaServico entidade) throws SQLException {
        
        String sql = "INSERT INTO tb_cad_categoria_servico (nome, descricao) VALUES (?, ?) RETURNING *";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getDescricao());
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                throw new SQLException("Não foi possível obter o cadastro inserido.");
            }
        }
    }

    @Override

    public Optional<CategoriaServico> buscarPorCodigo(long codigo) throws SQLException {
        String sql = "SELECT * FROM tb_cad_categoria_servico WHERE codigo = ?";
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

    public List<CategoriaServico> listar() throws SQLException {
        List<CategoriaServico> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cad_categoria_servico ORDER BY codigo";
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

    public boolean atualizar(long codigo, CategoriaServico entidade) throws SQLException {
        String sql = "UPDATE tb_cad_categoria_servico SET nome = ?, descricao = ? WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getDescricao());
            comando.setLong(3, codigo);
            return comando.executeUpdate() > 0;
        }
    }

    @Override

    public boolean excluir(long codigo) throws SQLException {
        String sql = "DELETE FROM tb_cad_categoria_servico WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigo);
            return comando.executeUpdate() > 0;
        }
    }


    private CategoriaServico mapear(ResultSet resultado) throws SQLException {
        return new CategoriaServico(
                resultado.getLong("codigo"),
                resultado.getString("nome"),
                resultado.getString("descricao"));
    }
}
