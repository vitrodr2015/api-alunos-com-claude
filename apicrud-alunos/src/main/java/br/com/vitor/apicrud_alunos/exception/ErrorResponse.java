package br.com.vitor.apicrud_alunos.exception;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "dataHora", "status", "erro", "mensagem" })
public record ErrorResponse(
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") Instant dataHora,
		int status, String erro, String mensagem) {
}
