package br.com.vitor.apicrud_alunos.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "id",  "ativo",  "email", "nome", "dataNascimento" })
public record AlunoResponse(Long id, String nome, String email,
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataNascimento, boolean ativo) {
}
