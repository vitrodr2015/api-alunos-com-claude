package br.com.vitor.apicrud_alunos.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

// BUSCA POR NOME ("começa com"), de ponta a ponta com o MySQL real: "ana" acha Ana Souza, mas não Mariana nem Tatiana.
// Usa um prefixo improvável (Zaza) para não se misturar com os alunos que a migração já cadastra.
class BuscaPorNomeIntegrationTests extends AbstractIntegrationTest {

	@LocalServerPort
	private int porta;

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

	private void criarAluno(String token, String nome) {
		given().auth().oauth2(token).contentType(ContentType.JSON)
				.body("{\"nome\":\"" + nome + "\",\"email\":\"busca." + UUID.randomUUID()
						+ "@escola.com\",\"dataNascimento\":\"2000-01-15\"}")
				.post("/alunos").then().statusCode(201);
	}

	@Test
	void buscaAchaSoQuemComecaComOTextoEIgnoraMaiusculas() {
		String token = token();
		criarAluno(token, "Zaza Souza");
		criarAluno(token, "Mazaza Lima"); // contém "zaza", mas não começa com ele
		criarAluno(token, "Tizaza Rocha"); // idem

		given().auth().oauth2(token).accept("application/hal+json").get("/alunos?nome=zaza")
				.then().statusCode(200)
				.body("page.totalElements", equalTo(1))
				.body("_embedded.alunos.nome", contains("Zaza Souza"));

		given().auth().oauth2(token).accept("application/hal+json").get("/alunos?nome=ZAZA")
				.then().statusCode(200)
				.body("_embedded.alunos.nome", contains("Zaza Souza"));
	}

	@Test
	void buscaNaoTrataPorcentoEUnderlineComoCuringa() {
		String token = token();
		criarAluno(token, "Ykyk Silva");

		// "%" e "_" são caracteres comuns na busca (o Spring Data os escapa), não "qualquer coisa"
		given().auth().oauth2(token).accept("application/hal+json").get("/alunos?nome=y_yk")
				.then().statusCode(200).body("page.totalElements", equalTo(0));
	}

}
