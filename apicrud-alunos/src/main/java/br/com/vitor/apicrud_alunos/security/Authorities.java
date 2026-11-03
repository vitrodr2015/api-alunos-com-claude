package br.com.vitor.apicrud_alunos.security;

// Nomes das permissões do sistema (as mesmas semeadas na migration V5). Constantes para não errar o texto
// nas regras de autorização: um "ADMlN" digitado errado compilaria e deixaria a rota fechada para todos.
public final class Authorities {

	public static final String ADMIN = "ADMIN";
	public static final String USER = "USER";

	private Authorities() {
	}

}
