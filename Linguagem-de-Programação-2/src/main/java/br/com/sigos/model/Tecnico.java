package br.com.sigos.model;

public final class Tecnico extends Pessoa {
    private final String especialidade;

    public Tecnico(Long codigo, String nome, String email, String especialidade) {
        super(codigo, nome, email);
        this.especialidade = Validacao.texto(especialidade, "Especialidade");
    }

    public String getEspecialidade() { return especialidade; }
    @Override public String getPapel() { return "TECNICO"; }
}
