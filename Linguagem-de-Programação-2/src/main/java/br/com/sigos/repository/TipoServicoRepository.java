package br.com.sigos.repository;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.model.TipoServico;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TipoServicoRepository implements Repositorio<TipoServico> {
    private final CarregarDados banco;


    public TipoServicoRepository(CarregarDados banco) {
        this.banco = banco;
    }

    @Override

    public TipoServico inserir(TipoServico entidade) throws SQLException {
        
        String sql = "INSERT INTO tb_cad_tipo_servico (codigo_categoria, nome, descricao) VALUES (?, ?, ?) RETURNING *";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, entidade.getCodigoCategoria());
            comando.setString(2, entidade.getNome());
            comando.setString(3, entidade.getDescricao());
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                throw new SQLException("Não foi possível obter o cadastro inserido.");
            }
        }
    }

    @Override

    public Optional<TipoServico> buscarPorCodigo(long codigo) throws SQLException {
        String sql = "SELECT * FROM tb_cad_tipo_servico WHERE codigo = ?";
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

    public List<TipoServico> listar() throws SQLException {
        List<TipoServico> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cad_tipo_servico ORDER BY codigo";
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

    public boolean atualizar(long codigo, TipoServico entidade) throws SQLException {
        String sql = "UPDATE tb_cad_tipo_servico SET codigo_categoria = ?, nome = ?, descricao = ? WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, entidade.getCodigoCategoria());
            comando.setString(2, entidade.getNome());
            comando.setString(3, entidade.getDescricao());
            comando.setLong(4, codigo);
            return comando.executeUpdate() > 0;
        }
    }

    @Override

    public boolean excluir(long codigo) throws SQLException {
        String sql = "DELETE FROM tb_cad_tipo_servico WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigo);
            return comando.executeUpdate() > 0;
        }
    }


    private TipoServico mapear(ResultSet resultado) throws SQLException {
        return new TipoServico(
                resultado.getLong("codigo"),
                resultado.getLong("codigo_categoria"),
                resultado.getString("nome"),
                resultado.getString("descricao"));
    }
}
