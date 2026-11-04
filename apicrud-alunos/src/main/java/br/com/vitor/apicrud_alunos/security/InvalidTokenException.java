package br.com.vitor.apicrud_alunos.security;

import org.springframework.security.core.AuthenticationException;

// Qualquer problema com um token (vencido, adulterado, de outro emissor, do tipo errado, de usuário que não existe
// mais ou está desativado/bloqueado) vira esta exceção. Ela herda de AuthenticationException, então o Spring Security
// a trata como falha de autenticação (HTTP 401). A mensagem é genérica de propósito: não ajuda quem tenta forjar tokens.
public class InvalidTokenException extends AuthenticationException {

	private static final long serialVersionUID = 1L;

	public InvalidTokenException(String message) {
		super(message);
	}

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}

}
