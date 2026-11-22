package br.com.sigos.model;

public abstract class Pessoa {
    private final Long codigo;
    private final String nome;
    private final String email;


    protected Pessoa(Long codigo, String nome, String email) {
        if (codigo != null) Validacao.codigo(codigo);
        this.codigo = codigo;
        this.nome = Validacao.texto(nome, "Nome");
        this.email = Validacao.texto(email, "Email");
        if (!this.email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Email inválido.");
    }

    public Long getCodigo() { return codigo; }

    public String getNome() { return nome; }

    public String getEmail() { return email; }

    public abstract String getPapel();
}
