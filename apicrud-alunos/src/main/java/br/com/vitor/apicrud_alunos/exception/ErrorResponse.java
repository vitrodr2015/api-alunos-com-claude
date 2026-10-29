package br.com.vitor.apicrud_alunos.exception;

import java.time.Instant;

public record ErrorResponse(Instant dataHora, int status, String erro, String mensagem) {
}
