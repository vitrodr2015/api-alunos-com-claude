package br.com.vitor.apicrud_alunos.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import br.com.vitor.apicrud_alunos.security.TokenPair;

@JsonPropertyOrder({ "tokenType", "accessToken", "accessExpiresAt", "refreshToken", "refreshExpiresAt" })
public record TokenResponse(
		String tokenType,
		String accessToken,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") Instant accessExpiresAt,
		String refreshToken,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC") Instant refreshExpiresAt) {

	public static TokenResponse from(TokenPair par) {
		return new TokenResponse("Bearer", par.accessToken(), par.accessExpiresAt(), par.refreshToken(), par.refreshExpiresAt());
	}

}
