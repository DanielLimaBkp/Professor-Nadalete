package br.com.sigos.repository;

import br.com.sigos.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrdemServicoRepository {

    public OrdemServico inserir(Connection conexao, OrdemServico ordem) throws SQLException {
        String sql = "INSERT INTO tb_ord_servico (codigo_cliente, codigo_tipo_servico, titulo, descricao, prioridade, status, data_abertura) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, ordem.getCodigoCliente());
            comando.setLong(2, ordem.getCodigoTipoServico());
            comando.setString(3, ordem.getTitulo());
            comando.setString(4, ordem.getDescricao());
            comando.setString(5, ordem.getPrioridade().name());
            comando.setString(6, ordem.getStatus().name());
            comando.setString(7, ordem.getDataAbertura().toString());
            try (var resultado = comando.executeQuery()) {
                if (resultado.next()) return mapear(resultado);
                throw new SQLException("Não foi possível obter a ordem.");
            }
        }
    }

    public OrdemServico buscarPorCodigo(Connection conexao, long codigo) throws SQLException {
        try (var comando = conexao.prepareStatement("SELECT * FROM tb_ord_servico WHERE codigo = ?")) {
            comando.setLong(1, codigo);
            try (var resultado = comando.executeQuery()) {
                return resultado.next() ? mapear(resultado) : null;
            }
        }
    }

    public List<OrdemServico> listar(Connection conexao, Long codigoCliente) throws SQLException {
        List<OrdemServico> lista = new ArrayList<>();
        String sql = codigoCliente == null ? "SELECT * FROM tb_ord_servico ORDER BY codigo DESC"
                : "SELECT * FROM tb_ord_servico WHERE codigo_cliente = ? ORDER BY codigo DESC";
        try (var comando = conexao.prepareStatement(sql)) {
            if (codigoCliente != null) comando.setLong(1, codigoCliente);
            try (var resultado = comando.executeQuery()) {
                while (resultado.next()) lista.add(mapear(resultado));
            }
        }
        return lista;
    }

    public void atualizar(Connection conexao, OrdemServico ordem) throws SQLException {
        String sql = "UPDATE tb_ord_servico SET codigo_cliente = ?, codigo_tipo_servico = ?, titulo = ?, descricao = ?, prioridade = ?, status = ? WHERE codigo = ?";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, ordem.getCodigoCliente());
            comando.setLong(2, ordem.getCodigoTipoServico());
            comando.setString(3, ordem.getTitulo());
            comando.setString(4, ordem.getDescricao());
            comando.setString(5, ordem.getPrioridade().name());
            comando.setString(6, ordem.getStatus().name());
            comando.setLong(7, ordem.getCodigo());
            comando.executeUpdate();
        }
    }

    public void excluir(Connection conexao, long codigo) throws SQLException {
        try (var comando = conexao.prepareStatement("DELETE FROM tb_ord_servico WHERE codigo = ?")) {
            comando.setLong(1, codigo);
            comando.executeUpdate();
        }
    }

    public List<Tecnico> listarTecnicoResponsavel(Connection conexao, long codigoOrdem) throws SQLException {
        List<Tecnico> lista = new ArrayList<>();
        String sql = "SELECT tb_cad_tecnico.* FROM tb_cad_tecnico JOIN tb_ord_tecnico ON tb_ord_tecnico.codigo_tecnico = tb_cad_tecnico.codigo WHERE tb_ord_tecnico.codigo_ordem = ? ORDER BY tb_cad_tecnico.nome, tb_cad_tecnico.codigo";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoOrdem);
            try (var r = comando.executeQuery()) {
                while (r.next()) lista.add(new Tecnico(r.getLong("codigo"), r.getString("nome"),
                        r.getString("email"), r.getString("especialidade")));
            }
        }
        return lista;
    }

    public void associarTecnico(Connection conexao, long codigoOrdem, long codigoTecnico) throws SQLException {
        String sql = "INSERT INTO tb_ord_tecnico (codigo_ordem, codigo_tecnico) VALUES (?, ?) ON CONFLICT(codigo_ordem, codigo_tecnico) DO NOTHING";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoOrdem);
            comando.setLong(2, codigoTecnico);
            comando.executeUpdate();
        }
    }

    public boolean removerTecnico(Connection conexao, long codigoOrdem, long codigoTecnico) throws SQLException {
        try (var comando = conexao.prepareStatement("DELETE FROM tb_ord_tecnico WHERE codigo_ordem = ? AND codigo_tecnico = ?")) {
            comando.setLong(1, codigoOrdem);
            comando.setLong(2, codigoTecnico);
            return comando.executeUpdate() > 0;
        }
    }

    private OrdemServico mapear(ResultSet r) throws SQLException {
        return new OrdemServico(r.getLong("codigo"), r.getLong("codigo_cliente"), r.getLong("codigo_tipo_servico"),
                r.getString("titulo"), r.getString("descricao"), Prioridade.valueOf(r.getString("prioridade")),
                StatusOrdem.valueOf(r.getString("status")), LocalDateTime.parse(r.getString("data_abertura")));
    }
}
