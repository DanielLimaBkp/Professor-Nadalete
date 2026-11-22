package br.com.sigos.repository;

import br.com.sigos.model.Tecnico;
import br.com.sigos.model.TipoServico;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TecnicoTipoServicoRepository {

    public List<TipoServico> listarTipoServicoHabilitado(Connection conexao, long codigoTecnico) throws SQLException {
        List<TipoServico> listagemTipoServicoHabilitado = new ArrayList<>();
        String sql = "SELECT tb_cad_tipo_servico.* FROM tb_cad_tipo_servico JOIN tb_cad_tecnico_tipo_servico ON tb_cad_tecnico_tipo_servico.codigo_tipo_servico = tb_cad_tipo_servico.codigo WHERE tb_cad_tecnico_tipo_servico.codigo_tecnico = ? ORDER BY tb_cad_tipo_servico.nome, tb_cad_tipo_servico.codigo";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoTecnico);
            try (var r = comando.executeQuery()) {
                while (r.next()) listagemTipoServicoHabilitado.add(new TipoServico(r.getLong("codigo"), r.getLong("codigo_categoria"), r.getString("nome"), r.getString("descricao")));
            }
        }
        return listagemTipoServicoHabilitado;
    }

    public List<Tecnico> listarTecnicoHabilitado(Connection conexao, long codigoTipoServico) throws SQLException {
        List<Tecnico> listagemTecnicoHabilitado = new ArrayList<>();
        String sql = "SELECT tb_cad_tecnico.* FROM tb_cad_tecnico JOIN tb_cad_tecnico_tipo_servico ON tb_cad_tecnico_tipo_servico.codigo_tecnico = tb_cad_tecnico.codigo WHERE tb_cad_tecnico_tipo_servico.codigo_tipo_servico = ? ORDER BY tb_cad_tecnico.nome, tb_cad_tecnico.codigo";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoTipoServico);
            try (var r = comando.executeQuery()) {
                while (r.next()) listagemTecnicoHabilitado.add(new Tecnico(r.getLong("codigo"), r.getString("nome"), r.getString("email"), r.getString("especialidade")));
            }
        }
        return listagemTecnicoHabilitado;
    }

    public boolean estaHabilitado(Connection conexao, long codigoTecnico, long codigoTipoServico) throws SQLException {
        try (var comando = conexao.prepareStatement("SELECT 1 FROM tb_cad_tecnico_tipo_servico WHERE codigo_tecnico = ? AND codigo_tipo_servico = ?")) {
            comando.setLong(1, codigoTecnico);
            comando.setLong(2, codigoTipoServico);
            try (var resultado = comando.executeQuery()) { return resultado.next(); }
        }
    }

    public void associar(Connection conexao, long codigoTecnico, long codigoTipoServico) throws SQLException {
        String sql = "INSERT INTO tb_cad_tecnico_tipo_servico (codigo_tecnico, codigo_tipo_servico) VALUES (?, ?) ON CONFLICT(codigo_tecnico, codigo_tipo_servico) DO NOTHING";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoTecnico);
            comando.setLong(2, codigoTipoServico);
            comando.executeUpdate();
        }
    }

    public boolean usadaEmOrdemAberta(Connection conexao, long codigoTecnico, long codigoTipoServico) throws SQLException {
        String sql = "SELECT 1 FROM tb_ord_servico JOIN tb_ord_tecnico ON tb_ord_tecnico.codigo_ordem = tb_ord_servico.codigo WHERE tb_ord_tecnico.codigo_tecnico = ? AND tb_ord_servico.codigo_tipo_servico = ? AND tb_ord_servico.status IN ('ABERTA', 'EM_ANDAMENTO') LIMIT 1";
        try (var comando = conexao.prepareStatement(sql)) {
            comando.setLong(1, codigoTecnico);
            comando.setLong(2, codigoTipoServico);
            try (var resultado = comando.executeQuery()) { return resultado.next(); }
        }
    }

    public boolean remover(Connection conexao, long codigoTecnico, long codigoTipoServico) throws SQLException {
        try (var comando = conexao.prepareStatement("DELETE FROM tb_cad_tecnico_tipo_servico WHERE codigo_tecnico = ? AND codigo_tipo_servico = ?")) {
            comando.setLong(1, codigoTecnico);
            comando.setLong(2, codigoTipoServico);
            return comando.executeUpdate() > 0;
        }
    }
}
