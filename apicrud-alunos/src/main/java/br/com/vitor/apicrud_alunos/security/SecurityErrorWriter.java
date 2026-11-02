package br.com.vitor.apicrud_alunos.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import br.com.vitor.apicrud_alunos.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

// Os erros da cadeia de segurança (401 e 403) nascem nos filtros, antes de qualquer controller, então o
// GlobalExceptionHandler não os alcança. Este componente escreve o mesmo ErrorResponse em JSON, para o cliente
// receber sempre o mesmo formato de erro.
@Component
public class SecurityErrorWriter {

	private final ObjectMapper objectMapper;

	public SecurityErrorWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void write(HttpServletResponse response, HttpStatus status, String erro, String mensagem) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(Instant.now(), status.value(), erro, mensagem));
	}

}
