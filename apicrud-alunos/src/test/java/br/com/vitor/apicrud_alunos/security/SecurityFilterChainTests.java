package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import jakarta.persistence.EntityManager;

// A cadeia de segurança inteira (JwtTokenFilter + SecurityConfig) com o banco de verdade.
// Cada teste roda numa transação desfeita no fim (nada fica no banco).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityFilterChainTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider provider;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private EntityManager em;

	@Value("${security.jwt.secret}")
	private String segredo;

	@Value("${security.jwt.issuer}")
	private String emissor;

	private User usuario(Permission... permissoes) {
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword("{bcrypt}hash-ficticio");
		u.setPermissions(List.of(permissoes));
		em.persist(u);
		em.flush();
		em.clear();
		return u;
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	// token de um usuário com a permissão USER (a mais básica): estar logado não basta, a rota exige permissão
	private String accessTokenValido() {
		Permission user = permissionRepository.findByDescription(Authorities.USER).orElseThrow();
		return provider.createTokenPair(usuario(user)).accessToken();
	}

	private static ResultMatcher naoE401() {
		return resultado -> assertThat(resultado.getResponse().getStatus()).isNotEqualTo(401);
	}

	// ---------- sem token / token ruim: 401 em JSON ----------

	@Test
	void semTokenAListagemDa401EmJsonNoFormatoErrorResponse() throws Exception {
		mockMvc.perform(get("/alunos"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(header().doesNotExist("WWW-Authenticate"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.erro").value("Não autenticado"))
				.andExpect(jsonPath("$.dataHora").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")));
	}

	@Test
	void todosOsMetodosEmRotasProtegidasSemTokenDao401() throws Exception {
		mockMvc.perform(get("/alunos/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/alunos/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/alunos/1/disable")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/alunos/1")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/rota-que-nao-existe")).andExpect(status().isUnauthorized());
	}

	@Test
	void swaggerNaoEPublicoPorqueOProjetoNaoTem() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
	}

	@Test
	void registerNaoEPublico() throws Exception {
		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenVencidoDa401() throws Exception {
		JwtTokenProvider vencido = new JwtTokenProvider(segredo, emissor, Duration.ofMinutes(-5), Duration.ofHours(1),
				userDetailsService);
		String token = vencido.createTokenPair(usuario()).accessToken();

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("Não autenticado"));
	}

	@Test
	void tokenAdulteradoDa401() throws Exception {
		String token = accessTokenValido();
		String adulterado = token.substring(0, token.length() - 3) + (token.endsWith("AAA") ? "BBB" : "AAA");

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(adulterado)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenDeOutroEmissorOuOutroSegredoDa401() throws Exception {
		User u = usuario();
		JwtTokenProvider outroEmissor = new JwtTokenProvider(segredo, "outro-sistema", Duration.ofMinutes(15),
				Duration.ofHours(1), userDetailsService);
		JwtTokenProvider outroSegredo = new JwtTokenProvider("outro-segredo-tambem-com-mais-de-32-caracteres!!", emissor,
				Duration.ofMinutes(15), Duration.ofHours(1), userDetailsService);

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(outroEmissor.createTokenPair(u).accessToken())))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(outroSegredo.createTokenPair(u).accessToken())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void refreshTokenNaoVaiLeComoAccessToken() throws Exception {
		String refresh = provider.createTokenPair(usuario()).refreshToken();

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(refresh)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void cabecalhosForaDoPadraoBearerDao401() throws Exception {
		String token = accessTokenValido();

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpzZW5oYQ==")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer   ")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer lixo.lixo.lixo")).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenEmParametroDaUrlNaoValeSoOCabecalho() throws Exception {
		mockMvc.perform(get("/alunos").param("access_token", accessTokenValido())).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenDeUsuarioDesativadoOuApagadoDa401() throws Exception {
		User desativado = usuario();
		String tokenDoDesativado = provider.createTokenPair(desativado).accessToken();
		User apagado = usuario();
		String tokenDoApagado = provider.createTokenPair(apagado).accessToken();

		em.find(User.class, desativado.getId()).setEnabled(false);
		em.remove(em.find(User.class, apagado.getId()));
		em.flush();
		em.clear();

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(tokenDoDesativado)))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(tokenDoApagado)))
				.andExpect(status().isUnauthorized());
	}

	// ---------- token bom: a API funciona normalmente ----------

	@Test
	void comTokenValidoAListagemFuncionaEmHal() throws Exception {
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(accessTokenValido())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(1))
				.andExpect(jsonPath("$.page.size").value(20));
	}

	@Test
	void esquemaBearerNaoDiferenciaMaiusculasDeMinusculas() throws Exception {
		mockMvc.perform(get("/alunos/1").header(HttpHeaders.AUTHORIZATION, "bearer " + accessTokenValido()))
				.andExpect(status().isOk());
	}

	@Test
	void escritasComTokenFuncionamSemPrecisarDeTokenCsrf() throws Exception {
		String token = accessTokenValido();
		String email = "seg-" + UUID.randomUUID() + "@escola.com";
		String corpo = "{\"nome\":\"Aluno Seguro\",\"email\":\"" + email + "\",\"dataNascimento\":\"2000-01-01\"}";

		String resposta = mockMvc.perform(post("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String id = resposta.replaceAll(".*\"id\":(\\d+).*", "$1");

		mockMvc.perform(put("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON).content(corpo.replace("Aluno Seguro", "Aluno Alterado")))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/alunos/" + id + "/disable").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
		// apagar é só do ADMIN: o USER recebe 403 e o ADMIN consegue (a matriz completa está em AuthorizationTests)
		mockMvc.perform(delete("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());
		Permission admin = permissionRepository.findByDescription(Authorities.ADMIN).orElseThrow();
		String tokenAdmin = provider.createTokenPair(usuario(admin)).accessToken();
		mockMvc.perform(delete("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
				.andExpect(status().isNoContent());
	}

	@Test
	void errosDoNegocioContinuamNoFormatoErrorResponseParaQuemTemToken() throws Exception {
		String token = accessTokenValido();

		mockMvc.perform(get("/alunos/99999").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.erro").value("Não encontrado"));
		mockMvc.perform(get("/alunos").param("sort", "senha").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isBadRequest());
		// rota sem regra de autorização é NEGADA (403), mesmo para quem está logado; não vira 404
		mockMvc.perform(get("/rota-que-nao-existe").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden());
	}

	// ---------- stateless ----------

	@Test
	void naoCriaSessaoNemCookie() throws Exception {
		var resultado = mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(accessTokenValido())))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(resultado.getRequest().getSession(false)).isNull();
		assertThat(resultado.getResponse().getHeader("Set-Cookie")).isNull();
	}

	// ---------- rotas públicas (a lista inteira) ----------

	@Test
	void rotasPublicasNaoDao401() throws Exception {
		// OPTIONS em qualquer caminho (pré-voo de CORS) e POST em /auth/signin e /auth/refresh.
		// Os controllers de /auth ainda não existem: a rota é liberada, mas responde 4xx (não 401).
		mockMvc.perform(options("/alunos")).andExpect(naoE401());
		mockMvc.perform(options("/qualquer/caminho")).andExpect(naoE401());
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(naoE401());
		mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(naoE401());
	}

	@Test
	void signinERefreshSoSaoPublicosParaPost() throws Exception {
		mockMvc.perform(get("/auth/signin")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void umaRotaPublicaIgnoraUmTokenRuim() throws Exception {
		mockMvc.perform(post("/auth/signin").header(HttpHeaders.AUTHORIZATION, "Bearer lixo")
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(naoE401());
	}

}
