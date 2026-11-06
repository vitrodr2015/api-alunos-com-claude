package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// Swagger LIGADO (SWAGGER_ENABLED=true): o que a documentação publica e o que ela NÃO abre.
// Desligado (o padrão) é testado em SecurityFilterChainTests e SensitivePathsTests.
@SpringBootTest(properties = { "springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true" })
@AutoConfigureMockMvc
class OpenApiDocsTests {

	@Autowired
	private MockMvc mockMvc;

	// o objeto JSON (mapa) tem todas estas chaves
	private static Matcher<Map<?, ?>> chaves(String... nomes) {
		return new TypeSafeMatcher<>() {
			@Override
			protected boolean matchesSafely(Map<?, ?> mapa) {
				return Arrays.stream(nomes).allMatch(mapa::containsKey);
			}

			@Override
			public void describeTo(Description d) {
				d.appendText("um objeto com as chaves ").appendValue(Arrays.toString(nomes));
			}
		};
	}

	@Test
	void adocumentacaoTemTituloVersaoEComoAutenticar() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("API de Alunos"))
				.andExpect(jsonPath("$.info.version").value("v1"))
				.andExpect(jsonPath("$.info.description", containsString("Authorize")))
				.andExpect(jsonPath("$.info.description", containsString("/auth/signin")))
				.andExpect(jsonPath("$.info.description", containsString("HAL")))
				.andExpect(jsonPath("$.info.description", containsString("sort=nome,asc")));
	}

	@Test
	void oEsquemaBearerJwtHabilitaOBotaoAuthorize() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				// por padrão toda rota pede o token
				.andExpect(jsonPath("$.security[0].bearerAuth", empty()));
	}

	@Test
	void existemDoisGruposUmPorRecurso() throws Exception {
		mockMvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
				.andExpect(jsonPath("$.urls[*].name", containsInAnyOrder("Alunos", "Autenticação")))
				.andExpect(jsonPath("$.urls[*].url", containsInAnyOrder("/v3/api-docs/alunos", "/v3/api-docs/autenticacao")));
	}

	@Test
	void ogrupoAlunosTemSoAsRotasDeAlunosEDocumentaCadaOperacao() throws Exception {
		mockMvc.perform(get("/v3/api-docs/alunos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", aMapWithSize(4))).andExpect(jsonPath("$.paths", chaves("/alunos", "/alunos/{id}", "/alunos/{id}/disable", "/alunos/{id}/enable")))
				.andExpect(jsonPath("$.paths['/alunos'].get.summary").value("Listar alunos"))
				.andExpect(jsonPath("$.paths['/alunos'].post.summary").value("Criar aluno"))
				.andExpect(jsonPath("$.paths['/alunos/{id}'].get.summary").value("Buscar aluno por id"))
				.andExpect(jsonPath("$.paths['/alunos/{id}'].put.summary").exists())
				.andExpect(jsonPath("$.paths['/alunos/{id}'].delete.summary").value("Apagar aluno (só ADMIN)"))
				.andExpect(jsonPath("$.paths['/alunos/{id}/disable'].patch.summary").value("Desativar aluno"))
				.andExpect(jsonPath("$.paths['/alunos/{id}/enable'].patch.summary").value("Reativar aluno"))
				.andExpect(jsonPath("$.tags[0].name").value("Alunos"));
	}

	@Test
	void asRespostasDeErroEstaoDocumentadasComOFormatoErrorResponse() throws Exception {
		mockMvc.perform(get("/v3/api-docs/alunos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/alunos/{id}'].delete.responses", chaves("204", "400", "401", "403", "404")))
				.andExpect(jsonPath("$.paths['/alunos'].post.responses", chaves("201", "400", "401", "409")))
				.andExpect(jsonPath("$.paths['/alunos'].get.responses", chaves("200", "400", "401")))
				.andExpect(jsonPath("$.components.schemas.ErrorResponse.properties", chaves("dataHora", "status", "erro", "mensagem")));
	}

	// o 503 (banco fora do ar) vale para QUALQUER rota e vem com Retry-After
	@Test
	void todasAsOperacoesDocumentamOServicoIndisponivel503() throws Exception {
		mockMvc.perform(get("/v3/api-docs/alunos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths.*.*.responses['503']", hasSize(7)))
				.andExpect(jsonPath("$.paths['/alunos'].get.responses['503'].headers['Retry-After']").exists())
				.andExpect(jsonPath("$.paths['/alunos/{id}'].delete.responses['503'].content['application/json'].schema.$ref").value("#/components/schemas/ErrorResponse"));
		mockMvc.perform(get("/v3/api-docs/autenticacao")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths.*.*.responses['503']", hasSize(3)));
	}

	@Test
	void ainformacaoDeDataRealEOrdenacaoAceitaEstaoNoTexto() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.description", containsString("2000-02-30")))
				.andExpect(jsonPath("$.info.description", containsString("503")))
				.andExpect(jsonPath("$.components.schemas.AlunoRequest.properties.dataNascimento.description", containsString("exista de verdade")));
		mockMvc.perform(get("/v3/api-docs/alunos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/alunos'].get.description", containsString("dataNascimento")));
	}

	@Test
	void aListagemDocumentaPaginacaoOrdenacaoEFiltros() throws Exception {
		mockMvc.perform(get("/v3/api-docs/alunos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/alunos'].get.parameters[*].name", hasItems("nome", "ativo", "page", "size", "sort")));
	}

	@Test
	void ogrupoAutenticacaoDeixaSigninERefreshSemCadeadoERegisterComCadeado() throws Exception {
		mockMvc.perform(get("/v3/api-docs/autenticacao")).andExpect(status().isOk())
				.andExpect(jsonPath("$.paths", aMapWithSize(3))).andExpect(jsonPath("$.paths", chaves("/auth/signin", "/auth/refresh", "/auth/register")))
				// @SecurityRequirements vazio = rota pública (sem cadeado na tela)
				.andExpect(jsonPath("$.paths['/auth/signin'].post.security", empty()))
				.andExpect(jsonPath("$.paths['/auth/refresh'].post.security", empty()))
				.andExpect(jsonPath("$.paths['/auth/register'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/auth/register'].post.responses", chaves("201", "400", "401", "403", "409")))
				.andExpect(jsonPath("$.tags[0].name").value("Autenticação"));
	}

	@Test
	void aTelaDoSwaggerAbreSemLogin() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
				.andExpect(content().string(containsString("swagger-ui")));
	}

	@Test
	void ligarADocumentacaoNaoAbreAApiNemOutrosMetodosNemSegredos() throws Exception {
		// as rotas de dados continuam exigindo o token
		mockMvc.perform(get("/alunos")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		// só o GET da documentação é público
		mockMvc.perform(post("/v3/api-docs")).andExpect(status().isUnauthorized());
		// a documentação não vaza o segredo do JWT nem nomes de classes internas
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(content().string(not(containsString("TESTE-nao-e-um-segredo"))))
				.andExpect(content().string(not(containsString("br.com.vitor"))));
	}

	@Test
	void osCorposDeEntradaTemExemplosUteisESemSenhaDeExemplo() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.schemas.AlunoRequest.properties.nome.example").value("Maria Silva"))
				.andExpect(jsonPath("$.components.schemas.AlunoRequest.properties.dataNascimento.example").value("2000-01-15"))
				.andExpect(jsonPath("$.components.schemas.SignInRequest.properties", chaves("username", "password")))
				// nenhum campo de senha traz valor de exemplo (nem o do login nem o do cadastro)
				.andExpect(jsonPath("$.components.schemas.SignInRequest.properties.password.example").doesNotExist())
				.andExpect(jsonPath("$.components.schemas.RegisterRequest.properties.password.example").doesNotExist());
	}

}
