package br.com.sigos.model;

import java.time.LocalDateTime;

public final class Atividade {
    private final Long codigo;
    private final long codigoOrdem;
    private final String descricao;

    private StatusAtividade status = StatusAtividade.PENDENTE;

    private LocalDateTime dataConclusao;

    public Atividade(Long codigo, long codigoOrdem, String descricao) {
        this(codigo, codigoOrdem, descricao, StatusAtividade.PENDENTE, null);
    }

    public Atividade(Long codigo, long codigoOrdem, String descricao,
                     StatusAtividade status, LocalDateTime dataConclusao) {
        if (codigo != null) Validacao.codigo(codigo);
        this.codigo = codigo;
        this.codigoOrdem = Validacao.codigo(codigoOrdem);
        this.descricao = Validacao.texto(descricao, "Descrição");
        if (status == null || (status == StatusAtividade.CONCLUIDA) != (dataConclusao != null))
            throw new IllegalArgumentException("Status e data de conclusão inconsistentes.");
        this.status = status;
        this.dataConclusao = dataConclusao;
    }

    public void alterarStatus(StatusAtividade novo) {
        if (novo == null) throw new IllegalArgumentException("Status é obrigatório.");
        if (novo == status) return;
        status = novo;
        dataConclusao = novo == StatusAtividade.CONCLUIDA ? LocalDateTime.now() : null;
    }

    public Long getCodigo() { return codigo; }

    public long getCodigoOrdem() { return codigoOrdem; }

    public String getDescricao() { return descricao; }

    public StatusAtividade getStatus() { return status; }

    public LocalDateTime getDataConclusao() { return dataConclusao; }
}
