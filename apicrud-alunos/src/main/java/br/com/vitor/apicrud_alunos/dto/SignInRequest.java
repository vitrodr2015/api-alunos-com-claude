package br.com.vitor.apicrud_alunos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignInRequest(
		@NotBlank(message = "deve ser informado") @Size(max = 100, message = "deve ter no máximo 100 caracteres") @Schema(description = "Nome de usuário", example = "admin") String username,
		@NotBlank(message = "deve ser informada") @Size(max = 200, message = "deve ter no máximo 200 caracteres") @Schema(description = "Senha do usuário") String password) {
}
