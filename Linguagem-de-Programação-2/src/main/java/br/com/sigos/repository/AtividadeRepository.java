package br.com.sigos.repository;

import br.com.sigos.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AtividadeRepository {

    public Atividade inserir(Connection conexao, Atividade atividade) throws SQLException {
        try (var comando = conexao.prepareStatement("INSERT INTO tb_ord_atividade (codigo_ordem, descricao, status) VALUES (?, ?, ?) RETURNING *")) {
            comando.setLong(1, atividade.getCodigoOrdem());
            comando.setString(2, atividade.getDescricao());
            comando.setString(3, atividade.getStatus().name());
            try (var resultado = comando.executeQuery()) {
                if (resultado.next()) return mapear(resultado);
                throw new SQLException("Não foi possível obter a atividade.");
            }
        }
    }

    public Atividade buscarPorCodigo(Connection conexao, long codigo) throws SQLException {
        try (var comando = conexao.prepareStatement("SELECT * FROM tb_ord_atividade WHERE codigo = ?")) {
            comando.setLong(1, codigo);
            try (var resultado = comando.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public List<Atividade> listar(Connection conexao, Long codigoOrdem) throws SQLException {
        List<Atividade> lista = new ArrayList<>();
        String sql = codigoOrdem == null ? "SELECT * FROM tb_ord_atividade ORDER BY codigo"
                : "SELECT * FROM tb_ord_atividade WHERE codigo_ordem = ? ORDER BY codigo";
        try (var comando = conexao.prepareStatement(sql)) {
            if (codigoOrdem != null) comando.setLong(1, codigoOrdem);
            try (var resultado = comando.executeQuery()) {
                while (resultado.next()) lista.add(mapear(resultado));
            }
        }
        return lista;
    }

    public void atualizar(Connection conexao, Atividade atividade) throws SQLException {
        try (var comando = conexao.prepareStatement("UPDATE tb_ord_atividade SET descricao = ?, status = ?, data_conclusao = ? WHERE codigo = ?")) {
            comando.setString(1, atividade.getDescricao());
            comando.setString(2, atividade.getStatus().name());
            comando.setString(3, atividade.getDataConclusao() == null ? null : atividade.getDataConclusao().toString());
            comando.setLong(4, atividade.getCodigo());
            comando.executeUpdate();
        }
    }

    public void excluir(Connection conexao, long codigo) throws SQLException {
        try (var comando = conexao.prepareStatement("DELETE FROM tb_ord_atividade WHERE codigo = ?")) {
            comando.setLong(1, codigo);
            comando.executeUpdate();
        }
    }

    private Atividade mapear(ResultSet r) throws SQLException {
        String data = r.getString("data_conclusao");
        return new Atividade(r.getLong("codigo"), r.getLong("codigo_ordem"), r.getString("descricao"),
                StatusAtividade.valueOf(r.getString("status")), data == null ? null : LocalDateTime.parse(data));
    }
}
