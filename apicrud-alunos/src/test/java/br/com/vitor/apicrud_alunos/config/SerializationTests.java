package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// A ordem dos campos do AlunoResponse vale em JSON, XML e YAML (nos endpoints que não são HAL, testados pelo
// PATCH /alunos/1/enable, idempotente) e também dentro do HAL do GET (_links primeiro, depois os campos).
@SpringBootTest
// Teste de contrato: os filtros de segurança ficam DESLIGADOS (addFilters = false), então este teste NÃO exercita a segurança.
// O 401 e o 403 são testados à parte, com os filtros ligados, em security/SecurityFilterChainTests.
@AutoConfigureMockMvc(addFilters = false)
class SerializationTests {

	private static final String DATA_HORA = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

	@Autowired
	private MockMvc mockMvc;

	// ---------- AlunoResponse puro (sem links) ----------

	@Test
	void jsonDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/json"))
				.andExpect(content().string(
						"{\"id\":1,\"ativo\":true,\"email\":\"ana.souza@escola.com\","
								+ "\"nome\":\"Ana Souza\",\"dataNascimento\":\"2000-05-10\"}"));
	}

	@Test
	void xmlDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/xml"))
				.andExpect(content().string(
						"<AlunoResponse><id>1</id><ativo>true</ativo><email>ana.souza@escola.com</email>"
								+ "<nome>Ana Souza</nome><dataNascimento>2000-05-10</dataNascimento></AlunoResponse>"));
	}

	@Test
	void yamlDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/yaml"))
				.andExpect(content().string(
						"---\nid: 1\nativo: true\nemail: \"ana.souza@escola.com\"\n"
								+ "nome: \"Ana Souza\"\ndataNascimento: \"2000-05-10\"\n"));
	}

	// ---------- HAL (GET) ----------

	@Test
	void halDoAlunoTrazOsLinksPrimeiroEDepoisOsCamposNaOrdemCombinada() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/hal+json"))
				.andExpect(content().string(
						"{\"_links\":{\"self\":{\"href\":\"http://localhost/alunos/1\"},"
								+ "\"alunos\":{\"href\":\"http://localhost/alunos\"},"
								+ "\"disable\":{\"href\":\"http://localhost/alunos/1/disable\"}},"
								+ "\"id\":1,\"ativo\":true,\"email\":\"ana.souza@escola.com\","
								+ "\"nome\":\"Ana Souza\",\"dataNascimento\":\"2000-05-10\"}"));
	}

	@Test
	void halDaPaginaTemEmbeddedLinksEPageNessaOrdem() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "1").accept("application/hal+json"))
				.andExpect(content().string(matchesPattern(
						"(?s)\\{\"_embedded\":\\{\"alunos\":\\[\\{\"_links\":\\{\"self\":\\{\"href\":\"http://localhost/alunos/1\"\\}.*\\}\\]\\},"
								+ "\"_links\":\\{\"first\":.*\"self\":.*\"next\":.*\"last\":.*\\},"
								+ "\"page\":\\{\"number\":0,\"size\":1,\"totalElements\":\\d+,\"totalPages\":\\d+\\}\\}")));
	}

	@Test
	void listaVaziaNoHalNaoTemEmbeddedSoLinksEPage() throws Exception {
		// a query string precisa estar na URL (o MockMvc não a monta a partir de .param()), pois os links nascem da URL
		mockMvc.perform(get("/alunos?nome=ninguem-com-este-nome").accept("application/hal+json"))
				.andExpect(content().string(
						"{\"_links\":{\"self\":{\"href\":\"http://localhost/alunos?nome=ninguem-com-este-nome"
								+ "&page=0&size=20&sort=id,asc\"}},"
								+ "\"page\":{\"number\":0,\"size\":20,\"totalElements\":0,\"totalPages\":0}}"));
	}

	// ---------- erro ----------

	@Test
	void dataHoraDoErroSaiPadronizadaEmMilissegundosUtc() throws Exception {
		mockMvc.perform(get("/alunos/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dataHora").value(matchesPattern(DATA_HORA)))
				.andExpect(content().string(org.hamcrest.Matchers.startsWith("{\"dataHora\":\"")))
				.andExpect(content().string(containsString("\",\"status\":404,\"erro\":\"Não encontrado\",\"mensagem\":")));
	}

}
