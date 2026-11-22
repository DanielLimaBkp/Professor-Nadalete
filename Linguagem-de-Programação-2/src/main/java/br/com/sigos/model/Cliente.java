package br.com.sigos.model;

public final class Cliente extends Pessoa {
    private final String documento;
    private final String telefone;

    public Cliente(Long codigo, String nome, String email, String documento, String telefone) {
        super(codigo, nome, email);
        this.documento = Validacao.texto(documento, "Documento");
        this.telefone = Validacao.texto(telefone, "Telefone");
    }

    public String getDocumento() { return documento; }

    public String getTelefone() { return telefone; }
    @Override public String getPapel() { return "CLIENTE"; }
}
