package br.com.vitor.apicrud_alunos.security;

import java.time.Instant;

// Par devolvido ao autenticar ou renovar: o access token (curto) vai em toda requisição; o refresh token (mais longo)
// só serve para pedir um novo par e deve ir no corpo de um POST, nunca na URL.
public record TokenPair(String accessToken, Instant accessExpiresAt, String refreshToken, Instant refreshExpiresAt) {
}
