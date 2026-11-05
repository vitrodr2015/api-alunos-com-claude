package br.com.vitor.apicrud_alunos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank(message = "deve ser informado")
		@Size(min = 3, max = 100, message = "deve ter de 3 a 100 caracteres")
		@Pattern(regexp = "^[A-Za-z0-9._@-]*$", message = "só pode ter letras, números e os símbolos . _ @ -")
		@Schema(description = "Nome de usuário único (3 a 100: letras, números e . _ @ -)", example = "maria.silva") String username,
		@NotBlank(message = "deve ser informado")
		@Size(min = 3, max = 150, message = "deve ter de 3 a 150 caracteres")
		@Schema(description = "Nome completo (3 a 150 caracteres)", example = "Maria Silva") String fullName,
		// o BCrypt só aceita até 72 BYTES; o tamanho em bytes (acentos contam mais) é conferido também no AuthService
		@NotBlank(message = "deve ser informada")
		@Size(min = 8, max = 72, message = "deve ter de 8 a 72 caracteres")
		@Schema(description = "Senha de 8 a 72 caracteres (nunca é devolvida)") String password) {
}
