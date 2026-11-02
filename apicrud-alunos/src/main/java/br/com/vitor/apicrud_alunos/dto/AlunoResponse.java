package br.com.vitor.apicrud_alunos.dto;

import java.time.LocalDate;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

// no HAL, a lista de alunos aparece em _embedded.alunos
@Relation(collectionRelation = "alunos", itemRelation = "aluno")
@JsonPropertyOrder({ "id",  "ativo",  "email", "nome", "dataNascimento" })
public record AlunoResponse(Long id, String nome, String email,
		@JsonFormat(pattern = "yyyy-MM-dd") LocalDate dataNascimento, boolean ativo) {
}
