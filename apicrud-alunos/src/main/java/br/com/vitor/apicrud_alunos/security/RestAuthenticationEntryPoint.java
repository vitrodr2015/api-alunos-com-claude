package br.com.vitor.apicrud_alunos.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 401: chamado quando uma rota protegida é acessada sem autenticação válida (sem token, token vencido, adulterado...).
// A mensagem é a mesma em todos os casos, de propósito: não revela por que o token foi recusado.
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final SecurityErrorWriter errorWriter;

	public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		errorWriter.write(response, HttpStatus.UNAUTHORIZED, "Não autenticado",
				"Autenticação necessária: envie o cabeçalho Authorization com um token Bearer válido");
	}

}
