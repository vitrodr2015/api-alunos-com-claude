package br.com.vitor.apicrud_alunos.security;

import org.springframework.security.core.AuthenticationException;

// Falha de login. É UMA SÓ, com a mesma mensagem, para usuário inexistente, senha errada, conta desativada ou
// bloqueada: mensagens diferentes revelariam quais usuários existem (enumeração de usuários).
public class InvalidCredentialsException extends AuthenticationException {

	private static final long serialVersionUID = 1L;

	public InvalidCredentialsException() {
		super("Credenciais inválidas");
	}

}
