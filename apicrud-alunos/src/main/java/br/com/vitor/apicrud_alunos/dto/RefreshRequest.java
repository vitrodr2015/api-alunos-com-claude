package br.com.vitor.apicrud_alunos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

// O refresh token vai no CORPO de um POST, nunca na URL (a URL fica em logs e no histórico do navegador)
public record RefreshRequest(@NotBlank(message = "deve ser informado") @Schema(description = "refreshToken devolvido pelo login (não o accessToken)") String refreshToken) {
}
