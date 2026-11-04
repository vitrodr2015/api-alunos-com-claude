package br.com.vitor.apicrud_alunos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;

// Os endpoints /auth com os filtros de segurança LIGADOS (nada de addFilters = false aqui) e o banco de verdade.
// Cada teste roda numa transação desfeita no fim (nada fica no banco).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTests {

	private static final String SENHA = "SenhaDeTeste#2026";
	private static final String ISO_MS = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider provider;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private EntityManager em;

	@Value("${security.jwt.secret}")
	private String segredo;

	@Value("${security.jwt.issuer}")
	private String emissor;

	// ---------- apoio ----------

	private User criarUsuario(String... permissoes) {
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword(passwordEncoder.encode(SENHA));
		List<Permission> lista = new ArrayList<>();
		for (String descricao : permissoes) {
			lista.add(permissionRepository.findByDescription(descricao).orElseThrow());
		}
		u.setPermissions(lista);
		em.persist(u);
		em.flush();
		em.clear();
		return u;
	}

	private String tokenDe(User u) {
		return provider.createTokenPair(u).accessToken();
	}

	private static String json(String chave1, String valor1, String chave2, String valor2) {
		return "{\"" + chave1 + "\":\"" + valor1 + "\",\"" + chave2 + "\":\"" + valor2 + "\"}";
	}

	private ResultActions signin(String usuario, String senha) throws Exception {
		return mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content(json("username", usuario, "password", senha)));
	}

	private ResultActions register(String bearer, String corpo) throws Exception {
		var requisicao = post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(corpo);
		if (bearer != null) {
			requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
		}
		return mockMvc.perform(requisicao);
	}

	private static String novoUsuarioJson(String nome, String nomeCompleto, String senha) {
		return "{\"username\":\"" + nome + "\",\"fullName\":\"" + nomeCompleto + "\",\"password\":\"" + senha + "\"}";
	}

	private static String nomeUnico() {
		return "novo-" + UUID.randomUUID().toString().substring(0, 8);
	}

	// ======================== POST /auth/signin ========================

	@Test
	void signinComCredenciaisCertasDevolveOsDoisTokens() throws Exception {
		User u = criarUsuario("USER");

		signin(u.getUsername(), SENHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(jsonPath("$.accessExpiresAt").value(matchesPattern(ISO_MS)))
				.andExpect(jsonPath("$.refreshExpiresAt").value(matchesPattern(ISO_MS)))
				.andExpect(header().string("Cache-Control", containsString("no-store")))
				.andExpect(header().string("Pragma", "no-cache"));
	}

	@Test
	void osTokensDoSigninFuncionamDeVerdade() throws Exception {
		User u = criarUsuario("USER");
		String resposta = signin(u.getUsername(), SENHA).andReturn().getResponse().getContentAsString();
		String access = resposta.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
				.andExpect(status().isOk());
	}

	@Test
	void signinNaoDevolveSenhaNemHash() throws Exception {
		User u = criarUsuario("USER");

		String corpo = signin(u.getUsername(), SENHA).andReturn().getResponse().getContentAsString();

		assertThat(corpo).doesNotContain(SENHA).doesNotContain("{bcrypt}").doesNotContain("password");
	}

	@Test
	void usuarioInexistenteESenhaErradaDaoExatamenteAMesmaResposta() throws Exception {
		User u = criarUsuario("USER");

		String inexistente = signin("fantasma-" + UUID.randomUUID(), SENHA).andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();
		String senhaErrada = signin(u.getUsername(), "senha-errada-qualquer").andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString();

		// só a data e hora muda; o resto (status, erro e mensagem) é idêntico
		assertThat(semDataHora(inexistente)).isEqualTo(semDataHora(senhaErrada));
		assertThat(inexistente).contains("\"erro\":\"Não autenticado\"").contains("\"mensagem\":\"Credenciais inválidas\"");
	}

	@Test
	void contaDesativadaBloqueadaOuExpiradaComASenhaCertaDaAMesmaRespostaGenerica() throws Exception {
		String referencia = semDataHora(signin("fantasma-" + UUID.randomUUID(), SENHA).andReturn().getResponse().getContentAsString());
		for (int caso = 0; caso < 4; caso++) {
			User u = criarUsuario("USER");
			User noBanco = em.find(User.class, u.getId());
			switch (caso) {
				case 0 -> noBanco.setEnabled(false);
				case 1 -> noBanco.setAccountNonLocked(false);
				case 2 -> noBanco.setAccountNonExpired(false);
				default -> noBanco.setCredentialsNonExpired(false);
			}
			em.flush();
			em.clear();

			String resposta = signin(u.getUsername(), SENHA).andExpect(status().isUnauthorized()).andReturn().getResponse()
					.getContentAsString();

			assertThat(semDataHora(resposta)).isEqualTo(referencia);
		}
	}

	@Test
	void senhaGigantesca200CaracteresDa401EnaoErro500() throws Exception {
		User u = criarUsuario("USER");

		signin(u.getUsername(), "x".repeat(200)).andExpect(status().isUnauthorized());
	}

	@Test
	void signinComCorpoInvalidoDa400() throws Exception {
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("username")))
				.andExpect(jsonPath("$.mensagem").value(containsString("password")));
		signin("", SENHA).andExpect(status().isBadRequest());
		signin("   ", SENHA).andExpect(status().isBadRequest());
		signin("alguem", "").andExpect(status().isBadRequest());
		signin("a".repeat(101), SENHA).andExpect(status().isBadRequest());
		signin("alguem", "x".repeat(201)).andExpect(status().isBadRequest());
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{quebrado"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void signinSoAceitaEResponderEmJson() throws Exception {
		mockMvc.perform(post("/auth/signin").contentType(MediaType.TEXT_PLAIN).content("x"))
				.andExpect(status().isUnsupportedMediaType());
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_XML)
				.content("{}")).andExpect(status().isNotAcceptable());
	}

	@Test
	void signinPorGetNaoEPublico() throws Exception {
		mockMvc.perform(get("/auth/signin")).andExpect(status().isUnauthorized());
	}

	// ======================== POST /auth/refresh ========================

	private String refreshDe(User u) {
		return provider.createTokenPair(u).refreshToken();
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refreshToken + "\"}"));
	}

	@Test
	void refreshValidoDevolveUmParNovoQueFunciona() throws Exception {
		User u = criarUsuario("USER");

		String resposta = refresh(refreshDe(u))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.refreshToken").isString())
				.andExpect(header().string("Cache-Control", containsString("no-store")))
				.andReturn().getResponse().getContentAsString();
		String novoAccess = resposta.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + novoAccess)).andExpect(status().isOk());
	}

	@Test
	void accessTokenNaoRenova() throws Exception {
		refresh(tokenDe(criarUsuario("USER")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.erro").value("Não autenticado"));
	}

	@Test
	void refreshVencidoAdulteradoOuLixoDa401() throws Exception {
		User u = criarUsuario("USER");
		JwtTokenProvider vencido = new JwtTokenProvider(segredo, emissor, Duration.ofMinutes(15), Duration.ofSeconds(-1),
				userDetailsService);
		String valido = refreshDe(u);
		String adulterado = valido.substring(0, valido.length() - 3) + (valido.endsWith("AAA") ? "BBB" : "AAA");

		refresh(vencido.createTokenPair(u).refreshToken()).andExpect(status().isUnauthorized());
		refresh(adulterado).andExpect(status().isUnauthorized());
		refresh("lixo.lixo.lixo").andExpect(status().isUnauthorized());
	}

	@Test
	void refreshDeUsuarioDesativadoOuApagadoDa401() throws Exception {
		User desativado = criarUsuario("USER");
		String refreshDoDesativado = refreshDe(desativado);
		User apagado = criarUsuario("USER");
		String refreshDoApagado = refreshDe(apagado);
		em.find(User.class, desativado.getId()).setEnabled(false);
		em.remove(em.find(User.class, apagado.getId()));
		em.flush();
		em.clear();

		refresh(refreshDoDesativado).andExpect(status().isUnauthorized());
		refresh(refreshDoApagado).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshSemCorpoOuSemOCampoDa400() throws Exception {
		mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("refreshToken")));
		refresh("").andExpect(status().isBadRequest());
	}

	@Test
	void refreshTokenNaUrlNaoValeSoNoCorpo() throws Exception {
		mockMvc.perform(post("/auth/refresh").param("refreshToken", refreshDe(criarUsuario("USER")))
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
	}

	// ======================== POST /auth/register ========================

	@Test
	void registerSemTokenDa401() throws Exception {
		register(null, novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123")).andExpect(status().isUnauthorized());
	}

	@Test
	void registerComTokenInvalidoDa401() throws Exception {
		User admin = criarUsuario("ADMIN");
		register(refreshDe(admin), novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123"))
				.andExpect(status().isUnauthorized());
		register("lixo.lixo.lixo", novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registerPorUsuarioSemAPermissaoAdminDa403EmJson() throws Exception {
		User comum = criarUsuario("USER");

		register(tokenDe(comum), novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123"))
				.andExpect(status().isForbidden())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.erro").value("Acesso negado"))
				.andExpect(jsonPath("$.mensagem").value("Você não tem permissão para acessar este recurso"));
	}

	@Test
	void registerPorUsuarioSemNenhumaPermissaoDa403() throws Exception {
		register(tokenDe(criarUsuario()), novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registerPorAdminCriaOUsuarioComAPermissaoPadraoUser() throws Exception {
		User admin = criarUsuario("ADMIN");
		String nome = nomeUnico();

		register(tokenDe(admin), novoUsuarioJson(nome, "Fulano de Tal", "SenhaForte#123"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value(nome))
				.andExpect(jsonPath("$.fullName").value("Fulano de Tal"))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.permissions", hasSize(1)))
				.andExpect(jsonPath("$.permissions", hasItem("USER")))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void registerGravaOHashDaSenhaENuncaOTextoEOUsuarioConsegueEntrar() throws Exception {
		User admin = criarUsuario("ADMIN");
		String nome = nomeUnico();
		register(tokenDe(admin), novoUsuarioJson(nome, "Fulano de Tal", "SenhaForte#123")).andExpect(status().isCreated());
		em.flush();
		em.clear();

		String gravado = (String) em.createNativeQuery("select password from users where user_name = ?1")
				.setParameter(1, nome).getSingleResult();

		assertThat(gravado).startsWith("{bcrypt}$2").doesNotContain("SenhaForte#123");
		signin(nome, "SenhaForte#123").andExpect(status().isOk());
		signin(nome, "SenhaForte#124").andExpect(status().isUnauthorized());
	}

	@Test
	void usuarioRecemCadastradoNaoEAdmin() throws Exception {
		User admin = criarUsuario("ADMIN");
		String nome = nomeUnico();
		register(tokenDe(admin), novoUsuarioJson(nome, "Fulano de Tal", "SenhaForte#123")).andExpect(status().isCreated());
		String resposta = signin(nome, "SenhaForte#123").andReturn().getResponse().getContentAsString();
		String access = resposta.replaceAll(".*\"accessToken\":\"([^\"]+)\".*", "$1");

		register(access, novoUsuarioJson(nomeUnico(), "Outro Fulano", "SenhaForte#123")).andExpect(status().isForbidden());
	}

	@Test
	void naoDaParaEscolherAsPermissoesNoCadastro() throws Exception {
		// tentativa de escalar privilégio mandando "permissions" no corpo: o campo é ignorado e só entra USER
		User admin = criarUsuario("ADMIN");
		String nome = nomeUnico();
		String corpo = "{\"username\":\"" + nome + "\",\"fullName\":\"Fulano de Tal\",\"password\":\"SenhaForte#123\","
				+ "\"permissions\":[\"ADMIN\"],\"enabled\":true,\"authorities\":[\"ADMIN\"]}";

		register(tokenDe(admin), corpo)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.permissions", hasSize(1)))
				.andExpect(jsonPath("$.permissions", hasItem("USER")))
				.andExpect(jsonPath("$.permissions", not(hasItem("ADMIN"))));
	}

	@Test
	void registerComNomeJaExistenteDa409InclusiveComOutraCaixa() throws Exception {
		User admin = criarUsuario("ADMIN");
		String nome = nomeUnico();
		register(tokenDe(admin), novoUsuarioJson(nome, "Fulano de Tal", "SenhaForte#123")).andExpect(status().isCreated());

		register(tokenDe(admin), novoUsuarioJson(nome, "Outro Nome", "SenhaForte#123"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.erro").value("Conflito"));
		register(tokenDe(admin), novoUsuarioJson(nome.toUpperCase(), "Outro Nome", "SenhaForte#123"))
				.andExpect(status().isConflict());
		register(tokenDe(admin), novoUsuarioJson(admin.getUsername(), "Outro Nome", "SenhaForte#123"))
				.andExpect(status().isConflict());
	}

	@Test
	void registerComDadosInvalidosDa400() throws Exception {
		String token = tokenDe(criarUsuario("ADMIN"));

		register(token, "{}").andExpect(status().isBadRequest());
		register(token, novoUsuarioJson("ab", "Fulano de Tal", "SenhaForte#123")).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson("com espaco", "Fulano de Tal", "SenhaForte#123")).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson("nome;drop", "Fulano de Tal", "SenhaForte#123")).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson(nomeUnico(), "", "SenhaForte#123")).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson(nomeUnico(), "Fulano de Tal", "curta")).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson(nomeUnico(), "Fulano de Tal", "x".repeat(73))).andExpect(status().isBadRequest());
		register(token, novoUsuarioJson(nomeUnico(), "Fulano de Tal", "        ")).andExpect(status().isBadRequest());
	}

	@Test
	void senhaDentroDe72CaracteresMasAcimaDe72BytesDa400EnaoErro500() throws Exception {
		// 40 letras acentuadas = 40 caracteres (passa no @Size) mas 80 bytes em UTF-8 (estoura o limite do BCrypt)
		register(tokenDe(criarUsuario("ADMIN")), novoUsuarioJson(nomeUnico(), "Fulano de Tal", "á".repeat(40)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("72 bytes")));
	}

	@Test
	void senhaDeExatamente72CaracteresFunciona() throws Exception {
		String nome = nomeUnico();
		String senha = "S3nh@-" + "a".repeat(66);
		assertThat(senha).hasSize(72);

		register(tokenDe(criarUsuario("ADMIN")), novoUsuarioJson(nome, "Fulano de Tal", senha)).andExpect(status().isCreated());

		signin(nome, senha).andExpect(status().isOk());
	}

	@Test
	void registerSoAceitaEResponderEmJson() throws Exception {
		String token = tokenDe(criarUsuario("ADMIN"));

		mockMvc.perform(post("/auth/register").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_XML).content("<x/>")).andExpect(status().isUnsupportedMediaType());
	}

	@Test
	void registerDeUsuarioAdminDesativadoDa401() throws Exception {
		User admin = criarUsuario("ADMIN");
		String token = tokenDe(admin);
		em.find(User.class, admin.getId()).setEnabled(false);
		em.flush();
		em.clear();

		register(token, novoUsuarioJson(nomeUnico(), "Fulano de Tal", "SenhaForte#123")).andExpect(status().isUnauthorized());
	}

	@Test
	void registerTemQueSerFeitoPorPostEOutrosMetodosDependemDaPermissao() throws Exception {
		// a regra de ADMIN vale para qualquer método em /auth/register
		mockMvc.perform(get("/auth/register")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/auth/register").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(criarUsuario("USER"))))
				.andExpect(status().isForbidden());
	}

	private static String semDataHora(String corpo) {
		return corpo.replaceAll("\"dataHora\":\"[^\"]+\",", "");
	}

}
