package br.com.vitor.apicrud_alunos.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

// Gera, valida e renova tokens JWT (HS256).
// - access token: curto, vai em toda requisição; refresh token: mais longo, só serve para renovar.
// - a claim "type" (access ou refresh) é conferida na leitura: um tipo não vale como o outro.
// - a cada validação o usuário é recarregado do banco: se foi apagado, desativado ou bloqueado, o token é recusado.
// Sem lista de revogação não existe "logout" de verdade: um token continua válido até expirar (por isso o access é curto).
@Component
public class JwtTokenProvider {

	static final String CLAIM_TYPE = "type";
	static final String CLAIM_ROLES = "roles";
	static final String TYPE_ACCESS = "access";
	static final String TYPE_REFRESH = "refresh";

	// HS256 pede um segredo de pelo menos 256 bits (32 bytes)
	static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

	private final Algorithm algorithm;
	private final String issuer;
	private final Duration accessTokenExpiration;
	private final Duration refreshTokenExpiration;
	private final UserDetailsService userDetailsService;

	public JwtTokenProvider(
			@Value("${security.jwt.secret}") String secret,
			@Value("${security.jwt.issuer}") String issuer,
			@Value("${security.jwt.access-token-expiration}") Duration accessTokenExpiration,
			@Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration,
			UserDetailsService userDetailsService) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"O segredo do JWT não foi definido. Defina a variável de ambiente JWT_SECRET antes de subir a aplicação.");
		}
		if (secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_DO_SEGREDO) {
			throw new IllegalStateException("O segredo do JWT (JWT_SECRET) é curto demais: use pelo menos "
					+ TAMANHO_MINIMO_DO_SEGREDO + " caracteres aleatórios.");
		}
		this.algorithm = Algorithm.HMAC256(secret);
		this.issuer = issuer;
		this.accessTokenExpiration = accessTokenExpiration;
		this.refreshTokenExpiration = refreshTokenExpiration;
		this.userDetailsService = userDetailsService;
	}

	// ---------- gerar ----------

	public TokenPair createTokenPair(UserDetails user) {
		Instant agora = Instant.now();
		Instant accessExpira = agora.plus(accessTokenExpiration);
		Instant refreshExpira = agora.plus(refreshTokenExpiration);
		return new TokenPair(
				gerar(user, TYPE_ACCESS, agora, accessExpira), accessExpira,
				gerar(user, TYPE_REFRESH, agora, refreshExpira), refreshExpira);
	}

	private String gerar(UserDetails user, String tipo, Instant emitidoEm, Instant expiraEm) {
		String[] roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toArray(String[]::new);
		return JWT.create()
				.withIssuer(issuer)
				.withSubject(user.getUsername())
				.withClaim(CLAIM_TYPE, tipo)
				.withArrayClaim(CLAIM_ROLES, roles)
				.withIssuedAt(emitidoEm)
				.withExpiresAt(expiraEm)
				.sign(algorithm);
	}

	// ---------- validar ----------

	// Valida um access token e devolve a autenticação com o usuário recarregado do banco
	public Authentication getAuthentication(String accessToken) {
		DecodedJWT jwt = verificar(accessToken, TYPE_ACCESS);
		UserDetails user = recarregarUsuario(jwt.getSubject());
		return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
	}

	// ---------- renovar ----------

	// Troca um refresh token válido por um novo par. Só o refresh serve (um access token não renova nada) e o usuário
	// é recarregado, então as permissões do token novo são as de agora, e não as do token antigo.
	public TokenPair renew(String refreshToken) {
		DecodedJWT jwt = verificar(refreshToken, TYPE_REFRESH);
		return createTokenPair(recarregarUsuario(jwt.getSubject()));
	}

	// ---------- internos ----------

	private DecodedJWT verificar(String token, String tipoEsperado) {
		if (token == null || token.isBlank()) {
			throw new InvalidTokenException("Token ausente");
		}
		DecodedJWT jwt;
		try {
			// confere a assinatura (HS256), o emissor, a data de expiração e o tipo
			jwt = JWT.require(algorithm)
					.withIssuer(issuer)
					.withClaim(CLAIM_TYPE, tipoEsperado)
					.build()
					.verify(token);
		} catch (TokenExpiredException e) {
			throw new InvalidTokenException("Token expirado", e);
		} catch (JWTVerificationException e) {
			throw new InvalidTokenException("Token inválido", e);
		}
		// um token assinado por nós mas sem validade ou sem usuário nunca deve valer
		if (jwt.getExpiresAt() == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
			throw new InvalidTokenException("Token inválido");
		}
		return jwt;
	}

	private UserDetails recarregarUsuario(String nome) {
		UserDetails user;
		try {
			user = userDetailsService.loadUserByUsername(nome);
		} catch (UsernameNotFoundException e) {
			throw new InvalidTokenException("Token inválido", e);
		}
		try {
			// recusa conta desativada, bloqueada, expirada ou com credenciais expiradas
			new AccountStatusUserDetailsChecker().check(user);
		} catch (AccountStatusException e) {
			throw new InvalidTokenException("Token inválido", e);
		}
		return user;
	}

}
