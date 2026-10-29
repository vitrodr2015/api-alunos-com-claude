package br.com.vitor.apicrud_alunos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record AlunoRequest(
		@NotBlank(message = "é obrigatório") @Size(min = 3, max = 100, message = "deve ter de 3 a 100 caracteres") String nome,
		@NotBlank(message = "é obrigatório") @Email(message = "deve ser um e-mail válido") String email,
		@NotNull(message = "é obrigatória") @Past(message = "deve ser uma data no passado") LocalDate dataNascimento) {
}
