package br.com.sigos.model;

public final class TipoServico {
    private final Long codigo;
    private final long codigoCategoria;
    private final String nome;
    private final String descricao;

    public TipoServico(Long codigo, long codigoCategoria, String nome, String descricao) {
        if (codigo != null) Validacao.codigo(codigo);
        this.codigo = codigo;
        this.codigoCategoria = Validacao.codigo(codigoCategoria);
        this.nome = Validacao.texto(nome, "Nome");
        this.descricao = Validacao.texto(descricao, "Descrição");
    }

    public Long getCodigo() { return codigo; }

    public long getCodigoCategoria() { return codigoCategoria; }

    public String getNome() { return nome; }

    public String getDescricao() { return descricao; }
}
