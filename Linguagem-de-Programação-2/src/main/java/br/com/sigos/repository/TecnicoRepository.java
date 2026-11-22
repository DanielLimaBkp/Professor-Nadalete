package br.com.sigos.repository;

import br.com.sigos.config.CarregarDados;
import br.com.sigos.model.Tecnico;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TecnicoRepository implements Repositorio<Tecnico> {
    private final CarregarDados banco;


    public TecnicoRepository(CarregarDados banco) {
        this.banco = banco;
    }

    @Override

    public Tecnico inserir(Tecnico entidade) throws SQLException {
        
        String sql = "INSERT INTO tb_cad_tecnico (nome, email, especialidade) VALUES (?, ?, ?) RETURNING *";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getEmail());
            comando.setString(3, entidade.getEspecialidade());
            try (ResultSet resultado = comando.executeQuery()) {
                if (resultado.next()) {
                    return mapear(resultado);
                }
                throw new SQLException("Não foi possível obter o cadastro inserido.");
            }
        }
    }

    @Override

    public Optional<Tecnico> buscarPorCodigo(long codigo) throws SQLException {
        String sql = "SELECT * FROM tb_cad_tecnico WHERE codigo = ?";
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

    public List<Tecnico> listar() throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT * FROM tb_cad_tecnico ORDER BY codigo";
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

    public boolean atualizar(long codigo, Tecnico entidade) throws SQLException {
        String sql = "UPDATE tb_cad_tecnico SET nome = ?, email = ?, especialidade = ? WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setString(1, entidade.getNome());
            comando.setString(2, entidade.getEmail());
            comando.setString(3, entidade.getEspecialidade());
            comando.setLong(4, codigo);
            return comando.executeUpdate() > 0;
        }
    }

    @Override

    public boolean excluir(long codigo) throws SQLException {
        String sql = "DELETE FROM tb_cad_tecnico WHERE codigo = ?";
        try (var conexao = banco.abrirConexao();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigo);
            return comando.executeUpdate() > 0;
        }
    }


    private Tecnico mapear(ResultSet resultado) throws SQLException {
        return new Tecnico(
                resultado.getLong("codigo"),
                resultado.getString("nome"),
                resultado.getString("email"),
                resultado.getString("especialidade"));
    }
}
