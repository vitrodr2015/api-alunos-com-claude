package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.AlunoRepository;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import jakarta.persistence.EntityManager;

// AUTORIZAÇÃO: não basta estar logado, a rota precisa de uma permissão. Matriz "quem pode o quê", com os filtros
// de segurança LIGADOS e o banco de verdade. Cada chamada usa um aluno novo (as de escrita o alteram ou apagam).
// Cada teste roda numa transação desfeita no fim (nada fica no banco).
//
// Regras (SecurityConfig):
//   leitura (GET/HEAD)                          USER ou ADMIN
//   criar, editar, ativar, desativar            USER ou ADMIN
//   apagar (DELETE)                             só ADMIN
//   qualquer rota sem regra                     NEGADA (403) para quem está logado; 401 para anônimo
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthorizationTests {

	// as chamadas da matriz, na ordem das colunas esperadas
	private static final List<String> ROTAS = List.of(
			"GET /alunos", "GET /alunos/{id}", "HEAD /alunos", "POST /alunos", "PUT /alunos/{id}",
			"PATCH /alunos/{id}/disable", "PATCH /alunos/{id}/enable", "DELETE /alunos/{id}",
			"GET /alunos/{id}/desconhecida (sem regra)", "POST /alunos/{id} (sem regra)", "GET /rota-que-nao-existe (sem regra)");

	private static final int[] ANONIMO = repetir(401);
	private static final int[] NEGADO_A_TODOS = repetir(403);
	private static final int[] USER = { 200, 200, 200, 201, 200, 200, 200, 403, 403, 403, 403 };
	private static final int[] ADMIN = { 200, 200, 200, 201, 200, 200, 200, 204, 403, 403, 403 };

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider provider;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private AlunoRepository alunoRepository;

	@Autowired
	private EntityManager em;

	// ---------- apoio ----------

	private static int[] repetir(int status) {
		int[] r = new int[ROTAS.size()];
		java.util.Arrays.fill(r, status);
		return r;
	}

	private User usuario(Permission... permissoes) {
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword("{bcrypt}hash-ficticio");
		u.setPermissions(new ArrayList<>(List.of(permissoes)));
		em.persist(u);
		em.flush();
		em.clear();
		return u;
	}

	private Permission permissao(String descricao) {
		return permissionRepository.findByDescription(descricao).orElseThrow();
	}

	private Permission permissaoNova() {
		Permission p = new Permission();
		p.setDescription("OUTRA_" + UUID.randomUUID().toString().substring(0, 8));
		em.persist(p);
		return p;
	}

	private String token(User u) {
		return provider.createTokenPair(u).accessToken();
	}

	private Long novoAluno() {
		Aluno a = new Aluno();
		String sufixo = UUID.randomUUID().toString().substring(0, 8);
		a.setNome("Aluno Autorizacao " + sufixo);
		a.setEmail("autorizacao-" + sufixo + "@escola.com");
		a.setDataNascimento(LocalDate.of(2000, 1, 1));
		return alunoRepository.saveAndFlush(a).getId();
	}

	private static String corpo() {
		String sufixo = UUID.randomUUID().toString().substring(0, 8);
		return "{\"nome\":\"Aluno Corpo " + sufixo + "\",\"email\":\"corpo-" + sufixo
				+ "@escola.com\",\"dataNascimento\":\"2001-02-03\"}";
	}

	private MockHttpServletRequestBuilder requisicao(int rota, Long id) {
		return switch (rota) {
			case 0 -> get("/alunos");
			case 1 -> get("/alunos/" + id);
			case 2 -> head("/alunos");
			case 3 -> post("/alunos").contentType(MediaType.APPLICATION_JSON).content(corpo());
			case 4 -> put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON).content(corpo());
			case 5 -> patch("/alunos/" + id + "/disable");
			case 6 -> patch("/alunos/" + id + "/enable");
			case 7 -> delete("/alunos/" + id);
			case 8 -> get("/alunos/" + id + "/desconhecida");
			case 9 -> post("/alunos/" + id).contentType(MediaType.APPLICATION_JSON).content("{}");
			default -> get("/rota-que-nao-existe");
		};
	}

	// executa a matriz inteira para um perfil e devolve as divergências (vazio = tudo como esperado)
	private List<String> divergencias(String tokenOuNulo, int[] esperado) throws Exception {
		List<String> problemas = new ArrayList<>();
		for (int rota = 0; rota < ROTAS.size(); rota++) {
			MockHttpServletRequestBuilder req = requisicao(rota, novoAluno());
			if (tokenOuNulo != null) {
				req.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOuNulo);
			}
			int obtido = mockMvc.perform(req).andReturn().getResponse().getStatus();
			if (obtido != esperado[rota]) {
				problemas.add(ROTAS.get(rota) + " -> esperado " + esperado[rota] + " mas veio " + obtido);
			}
		}
		return problemas;
	}

	// ---------- a matriz: um teste por perfil ----------

	@Test
	void anonimoTomaOito401EmTodasAsRotasDeDados() throws Exception {
		assertThat(divergencias(null, ANONIMO)).isEmpty();
	}

	@Test
	void logadoSemNenhumaPermissaoNaoAcessaNada() throws Exception {
		assertThat(divergencias(token(usuario()), NEGADO_A_TODOS)).isEmpty();
	}

	@Test
	void logadoComUmaPermissaoQueNaoEDeAlunosNaoAcessaNada() throws Exception {
		assertThat(divergencias(token(usuario(permissaoNova())), NEGADO_A_TODOS)).isEmpty();
	}

	@Test
	void userLeCriaEditaAtivaEDesativaMasNaoApaga() throws Exception {
		assertThat(divergencias(token(usuario(permissao("USER"))), USER)).isEmpty();
	}

	@Test
	void adminFazTudoInclusiveApagar() throws Exception {
		assertThat(divergencias(token(usuario(permissao("ADMIN"))), ADMIN)).isEmpty();
	}

	@Test
	void quemTemUserEAdminJuntosPodeTudoComoOAdmin() throws Exception {
		assertThat(divergencias(token(usuario(permissao("USER"), permissao("ADMIN"))), ADMIN)).isEmpty();
	}

	// ---------- detalhes que a matriz não mostra ----------

	@Test
	void ao403DoDeleteVemEmJsonENadaFoiApagado() throws Exception {
		Long id = novoAluno();
		String tokenUser = token(usuario(permissao("USER")));

		mockMvc.perform(delete("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser))
				.andExpect(status().isForbidden())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.erro").value("Acesso negado"))
				.andExpect(jsonPath("$.mensagem").value("Você não tem permissão para acessar este recurso"));
		assertThat(alunoRepository.existsById(id)).isTrue();
	}

	@Test
	void aAutorizacaoVemAntesDaExistenciaEntaoQuemNaoTemPermissaoNaoConsegueSondarIds() throws Exception {
		String semPermissao = token(usuario());

		// um id que não existe responde 403, e não 404: não dá para descobrir quais ids existem sem permissão
		mockMvc.perform(get("/alunos/99999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + semPermissao))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/alunos/99999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + semPermissao))
				.andExpect(status().isForbidden());
	}

	@Test
	void quemTemPermissaoVeOs404eOs400DeNegocioNormalmente() throws Exception {
		String tokenUser = token(usuario(permissao("USER")));

		mockMvc.perform(get("/alunos/99999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/alunos/abc").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser))
				.andExpect(status().isBadRequest());
	}

	// com os filtros LIGADOS, quem tem permissão também recebe o 409 de negócio (e-mail repetido, nome de usuário repetido)
	@Test
	void quemTemPermissaoVeOs409DeNegocioNormalmente() throws Exception {
		Long id = novoAluno();
		String email = alunoRepository.findById(id).orElseThrow().getEmail();
		String tokenUser = token(usuario(permissao("USER")));
		String corpoRepetido = "{\"nome\":\"Aluno Repetido\",\"email\":\"" + email.toUpperCase()
				+ "\",\"dataNascimento\":\"2000-01-15\"}";

		mockMvc.perform(post("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser)
				.contentType(MediaType.APPLICATION_JSON).content(corpoRepetido)).andExpect(status().isConflict());

		User admin = usuario(permissao("ADMIN"));
		String corpoUsuarioRepetido = "{\"username\":\"" + admin.getUserName().toUpperCase()
				+ "\",\"fullName\":\"Outro Nome\",\"password\":\"Senha-Boa-1234567\"}";
		mockMvc.perform(post("/auth/register").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(admin))
				.contentType(MediaType.APPLICATION_JSON).content(corpoUsuarioRepetido)).andExpect(status().isConflict());
	}

	@Test
	void permissaoRevogadaNoBancoValeNaProximaRequisicaoMesmoComOTokenAntigo() throws Exception {
		Permission admin = permissao("ADMIN");
		User u = usuario(admin);
		String tokenQueDizAdmin = token(u);
		Long id = novoAluno();
		// enquanto é ADMIN, pode apagar
		mockMvc.perform(delete("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenQueDizAdmin))
				.andExpect(status().isNoContent());

		// o ADMIN é revogado no banco; o token antigo ainda DIZ "ADMIN" nas roles, mas o que vale é o banco
		User noBanco = em.find(User.class, u.getId());
		noBanco.setPermissions(new ArrayList<>(List.of(permissao("USER"))));
		em.flush();
		em.clear();
		Long outro = novoAluno();

		mockMvc.perform(delete("/alunos/" + outro).header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenQueDizAdmin))
				.andExpect(status().isForbidden());
		assertThat(alunoRepository.existsById(outro)).isTrue();
	}

	@Test
	void tokenForjadoComRolesAdminMasUsuarioSemEssaPermissaoNoBancoNaoApaga() throws Exception {
		// o usuário do banco é só USER; o token é assinado por nós, mas com "roles":["ADMIN"] (forjado de dentro)
		User soUser = usuario(permissao("USER"));
		String forjado = com.auth0.jwt.JWT.create().withIssuer("apicrud-alunos").withSubject(soUser.getUsername())
				.withClaim("type", "access").withArrayClaim("roles", new String[] { "ADMIN" })
				.withExpiresAt(java.time.Instant.now().plusSeconds(600))
				.sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("TESTE-nao-e-um-segredo-real-0123456789abcdef"));
		Long id = novoAluno();

		mockMvc.perform(delete("/alunos/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + forjado))
				.andExpect(status().isForbidden());
		assertThat(alunoRepository.existsById(id)).isTrue();
	}

	@Test
	void rotasDeAutenticacaoContinuamComAsSuasProprias() throws Exception {
		// signin e refresh são públicos; register é só ADMIN (401 sem token, 403 para USER)
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(usuario(permissao("USER")))))
				.andExpect(status().isForbidden());
	}

}
