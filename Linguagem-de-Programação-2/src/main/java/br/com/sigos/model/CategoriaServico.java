package br.com.sigos.model;

public final class CategoriaServico {
    private final Long codigo;
    private final String nome;
    private final String descricao;

    public CategoriaServico(Long codigo, String nome, String descricao) {
        if (codigo != null) Validacao.codigo(codigo);
        this.codigo = codigo;
        this.nome = Validacao.texto(nome, "Nome");
        this.descricao = Validacao.texto(descricao, "Descrição");
    }

    public Long getCodigo() { return codigo; }

    public String getNome() { return nome; }

    public String getDescricao() { return descricao; }
    @Override

    public String toString() {
        return nome;
    }
}
