package br.com.vitor.apicrud_alunos.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

// CACHE NO REDIS, de ponta a ponta: aplicação inteira + MySQL real + Redis REAL (contêiner descartável, nunca o Redis
// do desenvolvedor). Nos demais testes o cache fica desligado (spring.cache.type=none); aqui ele é ligado.
@TestPropertySource(properties = "spring.cache.type=redis")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CacheRedisIntegrationTests extends AbstractIntegrationTest {

	private static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

	static {
		REDIS.start();
	}

	@DynamicPropertySource
	static void apontarParaORedisDoTeste(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
	}

	@LocalServerPort
	private int porta;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void apontarParaAAplicacao() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = porta;
	}

	private String token() {
		return given().contentType(ContentType.JSON)
				.body("{\"username\":\"" + ADMIN + "\",\"password\":\"" + SENHA_ADMIN + "\"}")
				.post("/auth/signin").then().statusCode(200).extract().path("accessToken");
	}

	private static String corpo(String nome) {
		return "{\"nome\":\"" + nome + "\",\"email\":\"cache." + UUID.randomUUID()
				+ "@escola.com\",\"dataNascimento\":\"2000-01-15\"}";
	}

	private int criarAluno(String token, String nome) {
		return given().auth().oauth2(token).contentType(ContentType.JSON).body(corpo(nome)).post("/alunos")
				.then().statusCode(201).extract().path("id");
	}

	private static String chave(int id) {
		return "aluno::" + id;
	}

	@Test
	void primeiroGetGravaNoRedisComValidadeESegundoGetVemDoCache() {
		String t = token();
		int id = criarAluno(t, "Cache Original");
		assertThat(redis.hasKey(chave(id))).isFalse(); // criar não coloca no cache

		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Cache Original"));

		assertThat(redis.hasKey(chave(id))).isTrue();
		Duration ttl = Duration.ofSeconds(redis.getExpire(chave(id)));
		assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(10));
		assertThat(redis.opsForValue().get(chave(id))).contains("\"@class\":\"br.com.vitor.apicrud_alunos.model.Aluno\"");

		// muda DIRETO no banco: se o GET ainda mostra o nome antigo, quem respondeu foi o cache (lendo o JSON do Redis)
		jdbc.update("UPDATE aluno SET nome = 'Mudou Direto No Banco' WHERE id = ?", id);
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Cache Original"));
	}

	@Test
	void putApagaOCacheEOGetSeguinteMostraODadoNovo() {
		String t = token();
		int id = criarAluno(t, "Antes Do Put");
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200);
		assertThat(redis.hasKey(chave(id))).isTrue();

		given().auth().oauth2(t).contentType(ContentType.JSON).body(corpo("Depois Do Put")).put("/alunos/{id}", id)
				.then().statusCode(200);

		assertThat(redis.hasKey(chave(id))).isFalse();
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Depois Do Put"));
	}

	@Test
	void desativarEAtivarApagamOCache() {
		String t = token();
		int id = criarAluno(t, "Liga Desliga");
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("ativo", equalTo(true));

		given().auth().oauth2(t).patch("/alunos/{id}/disable", id).then().statusCode(200);
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("ativo", equalTo(false));

		given().auth().oauth2(t).patch("/alunos/{id}/enable", id).then().statusCode(200);
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("ativo", equalTo(true));
	}

	@Test
	void deleteApagaOCacheEOGetSeguinteDa404() {
		String t = token();
		int id = criarAluno(t, "Vai Embora");
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200);
		assertThat(redis.hasKey(chave(id))).isTrue();

		given().auth().oauth2(t).delete("/alunos/{id}", id).then().statusCode(204);

		assertThat(redis.hasKey(chave(id))).isFalse();
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(404);
	}

	@Test
	void idInexistenteNaoEColocadoNoCache() {
		String t = token();
		given().auth().oauth2(t).get("/alunos/{id}", 987654321).then().statusCode(404);
		assertThat(redis.hasKey(chave(987654321))).isFalse();
	}

	// SEMPRE O ÚLTIMO (Integer.MAX_VALUE: o padrão dos outros é MAX_VALUE/2): derruba o Redis de verdade. A API tem de seguir funcionando (só sem cache) e rápido.
	@Test
	@Order(Integer.MAX_VALUE)
	void comORedisForaDoArAApiSegueFuncionandoConsultandoOBanco() {
		String t = token();
		int id = criarAluno(t, "Redis Vai Cair");
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200);

		REDIS.stop();

		jdbc.update("UPDATE aluno SET nome = 'Veio Do Banco' WHERE id = ?", id);
		long inicio = System.nanoTime();
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Veio Do Banco"));
		assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(6));

		given().auth().oauth2(t).contentType(ContentType.JSON).body(corpo("Alterado Sem Redis")).put("/alunos/{id}", id)
				.then().statusCode(200);
		given().auth().oauth2(t).get("/alunos/{id}", id).then().statusCode(200).body("nome", equalTo("Alterado Sem Redis"));
		given().get("/actuator/health").then().statusCode(200).body("status", equalTo("UP"));
	}
}
