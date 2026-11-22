package br.com.sigos.model;

final class Validacao {

    private Validacao() {}
    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(campo + " é obrigatório.");
        return valor.strip();
    }
    static long codigo(long valor) {
        if (valor <= 0) throw new IllegalArgumentException("Código deve ser positivo.");
        return valor;
    }
}
