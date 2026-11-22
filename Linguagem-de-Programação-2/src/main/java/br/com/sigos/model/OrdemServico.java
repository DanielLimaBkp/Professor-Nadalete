package br.com.sigos.model;

import java.time.LocalDateTime;
import java.util.List;

public final class OrdemServico {
    private final Long codigo;
    private final long codigoCliente;
    private final long codigoTipoServico;
    private final String titulo;
    private final String descricao;
    private final Prioridade prioridade;
    private final LocalDateTime dataAbertura;

    private StatusOrdem status;

    public OrdemServico(Long codigo, long codigoCliente, long codigoTipoServico,
                        String titulo, String descricao, Prioridade prioridade) {
        this(codigo, codigoCliente, codigoTipoServico, titulo, descricao, prioridade,
                StatusOrdem.ABERTA, LocalDateTime.now());
    }
    

    public OrdemServico(Long codigo, long codigoCliente, long codigoTipoServico,
                        String titulo, String descricao, Prioridade prioridade,
                        StatusOrdem status, LocalDateTime dataAbertura) {
        if (codigo != null) Validacao.codigo(codigo);
        this.codigo = codigo;
        this.codigoCliente = Validacao.codigo(codigoCliente);
        this.codigoTipoServico = Validacao.codigo(codigoTipoServico);
        this.titulo = Validacao.texto(titulo, "Título");
        this.descricao = Validacao.texto(descricao, "Descrição");
        if (prioridade == null) throw new IllegalArgumentException("Prioridade é obrigatória.");
        this.prioridade = prioridade;
        if (status == null || dataAbertura == null) throw new IllegalArgumentException("Estado e data são obrigatórios.");
        this.status = status;
        this.dataAbertura = dataAbertura;
    }

    public void validarEdicao() {
        if (status == StatusOrdem.CONCLUIDA || status == StatusOrdem.CANCELADA)
            throw new IllegalStateException("Ordem encerrada não pode ser alterada.");
    }
    

    public void alterarStatus(StatusOrdem novo, List<Atividade> listagemAtividade) {
        if (novo == null) throw new IllegalArgumentException("Status é obrigatório.");
        if (novo == status) return;
        validarEdicao();
        if (novo == StatusOrdem.CONCLUIDA) {
            if (listagemAtividade == null || codigo == null || listagemAtividade.stream().anyMatch(a ->
                    a == null || a.getCodigoOrdem() != codigo || a.getStatus() != StatusAtividade.CONCLUIDA))
                throw new IllegalStateException("Todas as atividades da ordem devem estar concluídas.");
        }
        status = novo;
    }

    public Long getCodigo() { return codigo; }

    public long getCodigoCliente() { return codigoCliente; }

    public long getCodigoTipoServico() { return codigoTipoServico; }

    public String getTitulo() { return titulo; }

    public String getDescricao() { return descricao; }

    public Prioridade getPrioridade() { return prioridade; }

    public StatusOrdem getStatus() { return status; }

    public LocalDateTime getDataAbertura() { return dataAbertura; }
}
