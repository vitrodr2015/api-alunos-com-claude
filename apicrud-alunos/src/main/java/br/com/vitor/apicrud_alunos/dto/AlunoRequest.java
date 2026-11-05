package br.com.vitor.apicrud_alunos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record AlunoRequest(
		@NotBlank(message = "é obrigatório") @Size(min = 3, max = 100, message = "deve ter de 3 a 100 caracteres") @Schema(description = "Nome completo (3 a 100 caracteres)", example = "Maria Silva") String nome,
		@NotBlank(message = "é obrigatório") @Email(message = "deve ser um e-mail válido") @Schema(description = "E-mail único (não diferencia maiúsculas)", example = "maria.silva@escola.com") String email,
		@NotNull(message = "é obrigatória") @Past(message = "deve ser uma data no passado")
		@JsonFormat(pattern = "yyyy-MM-dd") @Schema(description = "Data de nascimento (aaaa-mm-dd), no passado", example = "2000-01-15") LocalDate dataNascimento) {
}
