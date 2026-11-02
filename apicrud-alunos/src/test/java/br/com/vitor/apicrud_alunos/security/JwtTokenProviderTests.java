package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

// Testes unitários do provedor de tokens: sem Spring e sem banco (o UserDetailsService é simulado)
@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTests {

	private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-0123456789";
	private static final String EMISSOR = "apicrud-alunos";

	@Mock
	private UserDetailsService userDetailsService;

	private JwtTokenProvider provider;
	private UserDetails maria;

	@BeforeEach
	void preparar() {
		provider = new JwtTokenProvider(SEGREDO, EMISSOR, Duration.ofMinutes(15), Duration.ofHours(24), userDetailsService);
		maria = User.withUsername("maria").password("{bcrypt}hash-ficticio").authorities("ADMIN", "LEITURA").build();
	}

	private void usuarioExiste(UserDetails user) {
		when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
	}

	private static String base64(String texto) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
	}

	private static String semAssinatura(String payloadJson) {
		return base64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + base64(payloadJson) + ".";
	}

	// ---------- o que vai dentro do token ----------

	@Test
	void accessTokenTrazEmissorUsuarioRolesTipoEPrazoDe15Minutos() {
		DecodedJWT jwt = JWT.decode(provider.createTokenPair(maria).accessToken());

		assertThat(jwt.getIssuer()).isEqualTo(EMISSOR);
		assertThat(jwt.getSubject()).isEqualTo("maria");
		assertThat(jwt.getClaim("type").asString()).isEqualTo("access");
		assertThat(jwt.getClaim("roles").asList(String.class)).containsExactlyInAnyOrder("ADMIN", "LEITURA");
		assertThat(jwt.getIssuedAtAsInstant()).isNotNull();
		assertThat(Duration.between(jwt.getIssuedAtAsInstant(), jwt.getExpiresAtAsInstant())).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void refreshTokenTemTipoRefreshEPrazoMaisLongo() {
		DecodedJWT jwt = JWT.decode(provider.createTokenPair(maria).refreshToken());

		assertThat(jwt.getClaim("type").asString()).isEqualTo("refresh");
		assertThat(jwt.getSubject()).isEqualTo("maria");
		assertThat(Duration.between(jwt.getIssuedAtAsInstant(), jwt.getExpiresAtAsInstant())).isEqualTo(Duration.ofHours(24));
	}

	@Test
	void usuarioSemPermissoesGeraTokenComRolesVazio() {
		UserDetails semPermissoes = User.withUsername("joao").password("x").authorities(List.of()).build();

		assertThat(JWT.decode(provider.createTokenPair(semPermissoes).accessToken()).getClaim("roles").asList(String.class))
				.isEmpty();
	}

	@Test
	void oParDevolveOsInstantesDeExpiracaoCoerentesComOsTokens() {
		TokenPair par = provider.createTokenPair(maria);

		assertThat(par.accessExpiresAt()).isBefore(par.refreshExpiresAt());
		assertThat(par.accessExpiresAt().getEpochSecond())
				.isEqualTo(JWT.decode(par.accessToken()).getExpiresAtAsInstant().getEpochSecond());
	}

	// ---------- token válido ----------

	@Test
	void accessTokenValidoAutenticaOUsuarioComAsPermissoesDoBanco() {
		usuarioExiste(maria);

		Authentication autenticacao = provider.getAuthentication(provider.createTokenPair(maria).accessToken());

		assertThat(autenticacao.isAuthenticated()).isTrue();
		assertThat(autenticacao.getName()).isEqualTo("maria");
		assertThat(autenticacao.getPrincipal()).isSameAs(maria);
		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ADMIN", "LEITURA");
	}

	// ---------- "pronto quando": vencido, adulterado, outro emissor, tipo errado ----------

	@Test
	void tokenVencidoEhRecusado() {
		JwtTokenProvider vencido = new JwtTokenProvider(SEGREDO, EMISSOR, Duration.ofMinutes(-5), Duration.ofHours(1),
				userDetailsService);
		String token = vencido.createTokenPair(maria).accessToken();

		assertThatThrownBy(() -> provider.getAuthentication(token))
				.isInstanceOf(InvalidTokenException.class)
				.hasMessage("Token expirado");
	}

	@Test
	void tokenAdulteradoNaAssinaturaEhRecusado() {
		String token = provider.createTokenPair(maria).accessToken();
		String adulterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

		assertThatThrownBy(() -> provider.getAuthentication(adulterado))
				.isInstanceOf(InvalidTokenException.class)
				.hasMessage("Token inválido");
	}

	@Test
	void trocarAsRolesMantendoAAssinaturaOriginalEhRecusado() {
		// ataque de elevação de privilégio: o corpo do token é reescrito com ADMIN e a assinatura antiga é mantida
		UserDetails semPermissoes = User.withUsername("joao").password("x").authorities(List.of()).build();
		String[] partes = provider.createTokenPair(semPermissoes).accessToken().split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
		String payloadForjado = payload.replace("\"roles\":[]", "\"roles\":[\"ADMIN\"]");
		assertThat(payloadForjado).isNotEqualTo(payload);
		String forjado = partes[0] + "." + base64(payloadForjado) + "." + partes[2];

		assertThatThrownBy(() -> provider.getAuthentication(forjado)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenDeOutroEmissorEhRecusado() {
		JwtTokenProvider outroEmissor = new JwtTokenProvider(SEGREDO, "outro-sistema", Duration.ofMinutes(15),
				Duration.ofHours(1), userDetailsService);

		assertThatThrownBy(() -> provider.getAuthentication(outroEmissor.createTokenPair(maria).accessToken()))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenAssinadoComOutroSegredoEhRecusado() {
		JwtTokenProvider outroSegredo = new JwtTokenProvider("outro-segredo-tambem-com-mais-de-32-caracteres!!", EMISSOR,
				Duration.ofMinutes(15), Duration.ofHours(1), userDetailsService);

		assertThatThrownBy(() -> provider.getAuthentication(outroSegredo.createTokenPair(maria).accessToken()))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void refreshTokenNaoValeComoAccessToken() {
		assertThatThrownBy(() -> provider.getAuthentication(provider.createTokenPair(maria).refreshToken()))
				.isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void accessTokenNaoVaiLeComoRefreshToken() {
		assertThatThrownBy(() -> provider.renew(provider.createTokenPair(maria).accessToken()))
				.isInstanceOf(InvalidTokenException.class);
	}

	// ---------- outros ataques e tokens mal formados ----------

	@Test
	void tokenSemAssinaturaComAlgoritmoNoneEhRecusado() {
		long daquiAUmaHora = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();
		String semAssinar = semAssinatura("{\"iss\":\"" + EMISSOR + "\",\"sub\":\"maria\",\"type\":\"access\",\"exp\":"
				+ daquiAUmaHora + "}");

		assertThatThrownBy(() -> provider.getAuthentication(semAssinar)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenAssinadoPorNosMasSemDataDeValidadeEhRecusado() {
		// um token que nunca expira não pode valer, mesmo com assinatura certa
		String semExpiracao = JWT.create().withIssuer(EMISSOR).withSubject("maria").withClaim("type", "access")
				.sign(Algorithm.HMAC256(SEGREDO));

		assertThatThrownBy(() -> provider.getAuthentication(semExpiracao)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenAssinadoPorNosMasSemUsuarioEhRecusado() {
		String semUsuario = JWT.create().withIssuer(EMISSOR).withClaim("type", "access")
				.withExpiresAt(Instant.now().plus(Duration.ofHours(1))).sign(Algorithm.HMAC256(SEGREDO));

		assertThatThrownBy(() -> provider.getAuthentication(semUsuario)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void tokenAusenteOuMalFormadoEhRecusado() {
		for (String lixo : new String[] { null, "", "   ", "abc", "a.b.c", "Bearer abc.def.ghi" }) {
			assertThatThrownBy(() -> provider.getAuthentication(lixo)).isInstanceOf(InvalidTokenException.class);
		}
	}

	// ---------- o usuário é recarregado a cada validação ----------

	@Test
	void usuarioApagadoDepoisDeEmitirOTokenEhRecusado() {
		String token = provider.createTokenPair(maria).accessToken();
		when(userDetailsService.loadUserByUsername("maria")).thenThrow(new UsernameNotFoundException("não existe"));

		assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void usuarioDesativadoBloqueadoOuExpiradoDepoisDeEmitirOTokenEhRecusado() {
		String token = provider.createTokenPair(maria).accessToken();
		List<UserDetails> contasRuins = List.of(
				User.withUsername("maria").password("x").authorities("ADMIN").disabled(true).build(),
				User.withUsername("maria").password("x").authorities("ADMIN").accountLocked(true).build(),
				User.withUsername("maria").password("x").authorities("ADMIN").accountExpired(true).build(),
				User.withUsername("maria").password("x").authorities("ADMIN").credentialsExpired(true).build());

		for (UserDetails conta : contasRuins) {
			when(userDetailsService.loadUserByUsername("maria")).thenReturn(conta);
			assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(InvalidTokenException.class);
		}
	}

	@Test
	void aAutenticacaoUsaAsPermissoesDeAgoraEnaoAsDoToken() {
		String tokenComAdmin = provider.createTokenPair(maria).accessToken();
		UserDetails mariaSemAdmin = User.withUsername("maria").password("x").authorities("LEITURA").build();
		usuarioExiste(mariaSemAdmin);

		Authentication autenticacao = provider.getAuthentication(tokenComAdmin);

		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("LEITURA");
	}

	// ---------- renovar ----------

	@Test
	void refreshTokenValidoGeraUmNovoParComAsPermissoesAtuais() {
		String refresh = provider.createTokenPair(maria).refreshToken();
		UserDetails mariaSoLeitura = User.withUsername("maria").password("x").authorities("LEITURA").build();
		usuarioExiste(mariaSoLeitura);

		TokenPair novo = provider.renew(refresh);

		assertThat(JWT.decode(novo.accessToken()).getClaim("type").asString()).isEqualTo("access");
		assertThat(JWT.decode(novo.refreshToken()).getClaim("type").asString()).isEqualTo("refresh");
		assertThat(JWT.decode(novo.accessToken()).getClaim("roles").asList(String.class)).containsExactly("LEITURA");
		assertThat(provider.getAuthentication(novo.accessToken()).getName()).isEqualTo("maria");
	}

	@Test
	void refreshTokenVencidoNaoRenova() {
		JwtTokenProvider refreshCurto = new JwtTokenProvider(SEGREDO, EMISSOR, Duration.ofMinutes(15), Duration.ofSeconds(-1),
				userDetailsService);

		assertThatThrownBy(() -> provider.renew(refreshCurto.createTokenPair(maria).refreshToken()))
				.isInstanceOf(InvalidTokenException.class)
				.hasMessage("Token expirado");
	}

	@Test
	void refreshTokenDeUsuarioApagadoOuDesativadoNaoRenova() {
		String refresh = provider.createTokenPair(maria).refreshToken();
		when(userDetailsService.loadUserByUsername("maria"))
				.thenReturn(User.withUsername("maria").password("x").authorities("ADMIN").disabled(true).build());

		assertThatThrownBy(() -> provider.renew(refresh)).isInstanceOf(InvalidTokenException.class);
	}

	// ---------- o segredo ----------

	@Test
	void segredoAusenteOuVazioFazAAplicacaoFalharNaInicializacao() {
		for (String ruim : new String[] { null, "", "   " }) {
			assertThatThrownBy(() -> new JwtTokenProvider(ruim, EMISSOR, Duration.ofMinutes(15), Duration.ofHours(1),
					userDetailsService))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("JWT_SECRET");
		}
	}

	@Test
	void segredoCurtoDemaisEhRecusado() {
		assertThatThrownBy(() -> new JwtTokenProvider("x".repeat(31), EMISSOR, Duration.ofMinutes(15), Duration.ofHours(1),
				userDetailsService))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32");
	}

	@Test
	void segredoDeExatamente32BytesEhAceito() {
		assertThatCode(() -> new JwtTokenProvider("x".repeat(32), EMISSOR, Duration.ofMinutes(15), Duration.ofHours(1),
				userDetailsService)).doesNotThrowAnyException();
	}

}
