package br.com.vitor.apicrud_alunos.dto;

import java.time.LocalDate;

public record AlunoResponse(Long id, String nome, String email, LocalDate dataNascimento, boolean ativo) {
}
