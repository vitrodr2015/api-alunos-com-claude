package br.com.vitor.apicrud_alunos.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 403: o usuário está autenticado, mas não tem a permissão que a rota exige
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityErrorWriter errorWriter;

	public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		errorWriter.write(response, HttpStatus.FORBIDDEN, "Acesso negado",
				"Você não tem permissão para acessar este recurso");
	}

}
