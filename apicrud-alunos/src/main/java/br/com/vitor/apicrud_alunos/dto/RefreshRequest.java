package br.com.vitor.apicrud_alunos.dto;

import jakarta.validation.constraints.NotBlank;

// O refresh token vai no CORPO de um POST, nunca na URL (a URL fica em logs e no histórico do navegador)
public record RefreshRequest(@NotBlank(message = "deve ser informado") String refreshToken) {
}
