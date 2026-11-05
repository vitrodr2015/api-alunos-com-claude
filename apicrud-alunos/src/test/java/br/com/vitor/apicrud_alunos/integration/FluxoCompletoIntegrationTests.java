package br.com.vitor.apicrud_alunos.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

// NÍVEL DE INTEGRAÇÃO: contêiner MySQL, perfil "testcontainers" e bootstrap do admin vêm da classe base
// (AbstractIntegrationTest). Cada teste cria os próprios dados com nomes/e-mails únicos, então a ordem não importa.
class FluxoCompletoIntegrationTests extends AbstractIntegrationTest {

	@LocalServerPort
	private int porta;

	@Value("${security.jwt.secret}")
	private String segredo;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void apontarParaAAplicacao() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = porta;
	}

	// ---------- ajudantes ----------

	private static String json(String... paresChaveValor) {
		StringBuilder sb = new StringBuilder("{");
		for (int i = 0; i < paresChaveValor.length; i += 2) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append('"').append(paresChaveValor[i]).append("\":\"").append(paresChaveValor[i + 1]).append('"');
		}
		return sb.append('}').toString();
	}

	private Response entrar(String usuario, String senha) {
		return given().contentType(ContentType.JSON).body(json("username", usuario, "password", senha)).post("/auth/signin");
	}

	private String tokenDoAdmin() {
		return entrar(ADMIN, SENHA_ADMIN).then().statusCode(200).extract().path("accessToken");
	}

	// cria um usuário comum (só USER) pelo próprio endpoint e devolve o access token dele
	private String tokenDeUsuarioComum() {
		String nome = "comum." + UUID.randomUUID().toString().substring(0, 8);
		String senha = "Senha-Comum-" + UUID.randomUUID();
		given().auth().oauth2(tokenDoAdmin()).contentType(ContentType.JSON)
				.body(json("username", nome, "fullName", "Usuario Comum", "password", senha))
				.post("/auth/register").then().statusCode(201);
		return entrar(nome, senha).then().statusCode(200).extract().path("accessToken");
	}

	private static String emailUnico() {
		return "aluno." + UUID.randomUUID() + "@escola.com";
	}

	private static String alunoJson(String email) {
		return json("nome", "Aluno de Integracao", "email", email, "dataNascimento", "2000-01-15");
	}

	private int criarAluno(String token, String email) {
		return given().auth().oauth2(token).contentType(ContentType.JSON).body(alunoJson(email))
				.post("/alunos").then().statusCode(201).extract().path("id");
	}

	private String tokenForjado(String tipo, Instant expiraEm, String segredoUsado) {
		return JWT.create().withIssuer("apicrud-alunos").withSubject(ADMIN).withClaim("type", tipo)
				.withArrayClaim("roles", new String[] { "ADMIN", "USER" })
				.withIssuedAt(Instant.now().minusSeconds(3600)).withExpiresAt(expiraEm)
				.sign(Algorithm.HMAC256(segredoUsado));
	}

	// ---------- banco real: migrations ----------

	@Test
	void asMigrationsReaisDeV1aV6RodaramETerminaramSemLoginConhecido() {
		List<String> versoes = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class);
		assertThat(versoes).containsExactly("1", "2", "3", "4", "5", "6");

		List<String> permissoes = jdbc.queryForList("SELECT description FROM permission ORDER BY description", String.class);
		assertThat(permissoes).containsExactly("ADMIN", "USER");

		// o 'admin' semeado pela V5 foi removido pela V6; o único administrador é o do bootstrap
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE user_name = 'admin'", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE user_name = ?", Integer.class, ADMIN)).isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT password FROM users WHERE user_name = ?", String.class, ADMIN))
				.startsWith("{bcrypt}");
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM aluno", Integer.class)).isGreaterThanOrEqualTo(408);
	}

	// ---------- caso feliz: o fluxo completo ----------

	@Test
	void fluxoCompletoLoginCriarListarAtualizarDesativarAtivarEApagar() {
		Response login = entrar(ADMIN, SENHA_ADMIN);
		login.then().statusCode(200).body("tokenType", equalTo("Bearer")).body("accessToken", notNullValue())
				.body("refreshToken", notNullValue());
		String token = login.path("accessToken");
		String email = emailUnico();

		Response criado = given().auth().oauth2(token).contentType(ContentType.JSON).body(alunoJson(email)).post("/alunos");
		criado.then().statusCode(201).header("Location", containsString("/alunos/")).body("ativo", equalTo(true))
				.body("email", equalTo(email));
		int id = criado.path("id");

		given().auth().oauth2(token).get("/alunos/{id}", id).then().statusCode(200).body("id", equalTo(id));

		given().auth().oauth2(token).queryParam("page", 0).queryParam("size", 5).get("/alunos")
				.then().statusCode(200).body("_embedded.alunos.size()", equalTo(5)).body("page.size", equalTo(5))
				.body("page.totalElements", greaterThanOrEqualTo(409));

		given().auth().oauth2(token).queryParam("nome", "Integracao").get("/alunos")
				.then().statusCode(200).body("_embedded.alunos", not(empty()));

		given().auth().oauth2(token).contentType(ContentType.JSON)
				.body(json("nome", "Nome Atualizado", "email", email, "dataNascimento", "1999-12-31"))
				.put("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Nome Atualizado"))
				.body("dataNascimento", equalTo("1999-12-31"));

		given().auth().oauth2(token).patch("/alunos/{id}/disable", id).then().statusCode(200).body("ativo", equalTo(false));
		given().auth().oauth2(token).queryParam("ativo", false).queryParam("nome", "Nome Atualizado").get("/alunos")
				.then().statusCode(200).body("_embedded.alunos.id", hasItem(id));
		given().auth().oauth2(token).patch("/alunos/{id}/enable", id).then().statusCode(200).body("ativo", equalTo(true));

		given().auth().oauth2(token).delete("/alunos/{id}", id).then().statusCode(204);
		given().auth().oauth2(token).get("/alunos/{id}", id).then().statusCode(404);
	}

	@Test
	void usuarioComumPodeLerCriarEEditarMasONomeFicaNoBancoComHashBcrypt() {
		String nome = "comum." + UUID.randomUUID().toString().substring(0, 8);
		String senha = "Senha-Comum-" + UUID.randomUUID();
		given().auth().oauth2(tokenDoAdmin()).contentType(ContentType.JSON)
				.body(json("username", nome, "fullName", "Usuario Comum", "password", senha))
				.post("/auth/register").then().statusCode(201).body("permissions", equalTo(List.of("USER")))
				.body("$", not(hasKey("password")));
		assertThat(jdbc.queryForObject("SELECT password FROM users WHERE user_name = ?", String.class, nome))
				.startsWith("{bcrypt}").doesNotContain(senha);

		String token = entrar(nome, senha).then().statusCode(200).extract().path("accessToken");
		int id = criarAluno(token, emailUnico());
		given().auth().oauth2(token).get("/alunos/{id}", id).then().statusCode(200);
		given().auth().oauth2(token).patch("/alunos/{id}/disable", id).then().statusCode(200);
	}

	@Test
	void renovarOTokenDevolveUmParNovoQueFunciona() {
		Response login = entrar(ADMIN, SENHA_ADMIN);
		String refresh = login.path("refreshToken");
		Response renovado = given().contentType(ContentType.JSON).body(json("refreshToken", refresh)).post("/auth/refresh");
		renovado.then().statusCode(200).body("accessToken", notNullValue());
		given().auth().oauth2(renovado.<String>path("accessToken")).get("/alunos/1").then().statusCode(200);
	}

	// ---------- 400 ----------

	@Test
	void respondeComErro400EmJsonParaCorpoInvalidoEIdInvalido() {
		String token = tokenDoAdmin();
		given().auth().oauth2(token).contentType(ContentType.JSON).body("{}").post("/alunos")
				.then().statusCode(400).body("status", equalTo(400)).body("erro", notNullValue()).body("mensagem", notNullValue());
		given().auth().oauth2(token).contentType(ContentType.JSON).body("{nao e json").post("/alunos").then().statusCode(400);
		given().auth().oauth2(token).get("/alunos/0").then().statusCode(400);
		given().auth().oauth2(token).get("/alunos/abc").then().statusCode(400);
		given().auth().oauth2(token).queryParam("sort", "senha").get("/alunos").then().statusCode(400);
		given().contentType(ContentType.JSON).body(json("username", "", "password", "")).post("/auth/signin")
				.then().statusCode(400);
	}

	// ---------- 404 ----------

	@Test
	void respondeComErro404EmJsonParaAlunoQueNaoExiste() {
		String token = tokenDoAdmin();
		given().auth().oauth2(token).get("/alunos/99999999").then().statusCode(404).body("status", equalTo(404));
		given().auth().oauth2(token).delete("/alunos/99999999").then().statusCode(404);
		given().auth().oauth2(token).patch("/alunos/99999999/disable").then().statusCode(404);
	}

	// ---------- 409 (inclusive a regra do banco real) ----------

	@Test
	void respondeComErro409ParaEmailRepetidoMesmoComMaiusculasNoMySqlReal() {
		String token = tokenDoAdmin();
		String email = emailUnico();
		criarAluno(token, email);
		given().auth().oauth2(token).contentType(ContentType.JSON).body(alunoJson(email)).post("/alunos")
				.then().statusCode(409).body("status", equalTo(409));
		given().auth().oauth2(token).contentType(ContentType.JSON).body(alunoJson(email.toUpperCase())).post("/alunos")
				.then().statusCode(409);
	}

	@Test
	void respondeComErro409ParaNomeDeUsuarioRepetido() {
		String token = tokenDoAdmin();
		given().auth().oauth2(token).contentType(ContentType.JSON)
				.body(json("username", ADMIN.toUpperCase(), "fullName", "Outro Nome", "password", "Senha-Boa-1234567"))
				.post("/auth/register").then().statusCode(409);
	}

	// ---------- 401 ----------

	@Test
	void respondeComErro401SemTokenOuComLoginInvalido() {
		given().get("/alunos").then().statusCode(401).body("status", equalTo(401));
		given().get("/alunos/1").then().statusCode(401);
		given().contentType(ContentType.JSON).body(alunoJson(emailUnico())).post("/alunos").then().statusCode(401);
		given().delete("/alunos/1").then().statusCode(401);
		entrar(ADMIN, "senha-errada-123456").then().statusCode(401);
		entrar("ninguem.assim", "senha-errada-123456").then().statusCode(401);
		// a senha antiga semeada pela V5 (admin) não funciona mais: não existe login conhecido
		entrar("admin", "admin123").then().statusCode(401);
	}

	@Test
	void respondeComErro401ParaTokenAdulteradoVencidoDeOutroSegredoOuDoTipoErrado() {
		String bom = tokenDoAdmin();
		String adulterado = bom.substring(0, bom.length() - 2) + (bom.endsWith("AA") ? "BB" : "AA");
		given().auth().oauth2(adulterado).get("/alunos").then().statusCode(401);

		given().auth().oauth2(tokenForjado("access", Instant.now().minusSeconds(60), segredo)).get("/alunos")
				.then().statusCode(401);
		given().auth().oauth2(tokenForjado("access", Instant.now().plusSeconds(600), "OUTRO-segredo-de-32-caracteres-xxxxxxxx"))
				.get("/alunos").then().statusCode(401);
		given().auth().oauth2(tokenForjado("refresh", Instant.now().plusSeconds(600), segredo)).get("/alunos")
				.then().statusCode(401);
		// um access token não renova nada
		given().contentType(ContentType.JSON).body(json("refreshToken", bom)).post("/auth/refresh").then().statusCode(401);
		given().auth().oauth2("isto-nao-e-um-token").get("/alunos").then().statusCode(401);
	}

	@Test
	void umTokenValidoDeUmUsuarioQueDeixouDeExistirNaoEntra() {
		// token forjado com o segredo certo, mas para um usuário que não está no banco
		String token = JWT.create().withIssuer("apicrud-alunos").withSubject("fantasma").withClaim("type", "access")
				.withArrayClaim("roles", new String[] { "ADMIN" }).withIssuedAt(Instant.now())
				.withExpiresAt(Instant.now().plusSeconds(600)).sign(Algorithm.HMAC256(segredo));
		given().auth().oauth2(token).get("/alunos").then().statusCode(401);
	}

	// ---------- 403 ----------

	@Test
	void respondeComErro403QuandoOUsuarioComumTentaOQueSoOAdminPode() {
		String admin = tokenDoAdmin();
		int id = criarAluno(admin, emailUnico());
		String comum = tokenDeUsuarioComum();

		given().auth().oauth2(comum).delete("/alunos/{id}", id).then().statusCode(403).body("status", equalTo(403));
		given().auth().oauth2(admin).get("/alunos/{id}", id).then().statusCode(200); // nada foi apagado

		given().auth().oauth2(comum).contentType(ContentType.JSON)
				.body(json("username", "tentativa." + UUID.randomUUID().toString().substring(0, 6), "fullName", "Tentativa Nome",
						"password", "Senha-Boa-1234567"))
				.post("/auth/register").then().statusCode(403);
	}

	@Test
	void rotasSemRegraECaminhosSensiveisSaoNegadosAteParaOAdministrador() {
		String admin = tokenDoAdmin();
		for (String caminho : List.of("/actuator/env", "/actuator/beans", "/actuator/heapdump", "/h2-console", "/.env", "/rota-que-nao-existe")) {
			given().auth().oauth2(admin).get(caminho).then().statusCode(403);
			given().get(caminho).then().statusCode(401);
		}
	}

	// health check com o MySQL REAL: o /actuator/health completo inclui o banco; público, só o status, sem detalhes
	@Test
	void healthEPublicoEUPComOMySqlReal() {
		given().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP")).body("components", org.hamcrest.Matchers.nullValue());
		given().get("/actuator/health/liveness").then().statusCode(200).body("status", equalTo("UP"));
		given().get("/actuator/health/readiness").then().statusCode(200).body("status", equalTo("UP"));
		// só o health: o resto do Actuator continua fechado, até com token de ADMIN
		given().auth().oauth2(tokenDoAdmin()).get("/actuator/env").then().statusCode(403);
	}

	@Test
	void osErrosNuncaVazamDetalhesInternos() {
		String admin = tokenDoAdmin();
		List<String> corpos = List.of(
				given().auth().oauth2(admin).get("/alunos/abc").asString(),
				given().auth().oauth2(admin).get("/alunos/99999999").asString(),
				given().get("/alunos").asString(),
				given().auth().oauth2(tokenDeUsuarioComum()).delete("/alunos/1").asString());
		for (String corpo : corpos) {
			assertThat(corpo).isNotBlank().doesNotContain("Exception").doesNotContain("br.com.vitor")
					.doesNotContain("org.springframework").doesNotContain("com.mysql").doesNotContain("SQL");
		}
	}

	private static org.hamcrest.Matcher<java.util.Map<? extends String, ?>> hasKey(String chave) {
		return org.hamcrest.Matchers.hasKey(chave);
	}

}
