package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SerializationTests {

	private static final String DATA_HORA = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void jsonDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/json"))
				.andExpect(content().string(
						"{\"id\":1,\"ativo\":true,\"email\":\"ana.souza@escola.com\","
								+ "\"nome\":\"Ana Souza\",\"dataNascimento\":\"2000-05-10\"}"));
	}

	@Test
	void xmlDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/xml"))
				.andExpect(content().string(
						"<AlunoResponse><id>1</id><ativo>true</ativo><email>ana.souza@escola.com</email>"
								+ "<nome>Ana Souza</nome><dataNascimento>2000-05-10</dataNascimento></AlunoResponse>"));
	}

	@Test
	void yamlDoAlunoSaiNaOrdemCombinada() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/yaml"))
				.andExpect(content().string(
						"---\nid: 1\nativo: true\nemail: \"ana.souza@escola.com\"\n"
								+ "nome: \"Ana Souza\"\ndataNascimento: \"2000-05-10\"\n"));
	}

	@Test
	void paginaSaiNaOrdemCombinadaNosTresFormatos() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "1").accept("application/json"))
				.andExpect(content().string(matchesPattern(
						"\\{\"content\":\\[\\{\"id\":1,.*}],\"page\":0,\"size\":1,\"totalElements\":\\d+,\"totalPages\":\\d+}")));

		mockMvc.perform(get("/alunos").param("size", "1").accept("application/xml"))
				.andExpect(content().string(containsString("<PageResponse><content><item><id>1</id>")))
				.andExpect(content().string(containsString("</item></content><page>0</page><size>1</size>")))
				.andExpect(content().string(not(containsString("<content><content>"))));

		mockMvc.perform(get("/alunos").param("size", "1").accept("application/yaml"))
				.andExpect(content().string(containsString("---\ncontent:\n- id: 1\n")))
				.andExpect(content().string(containsString("page: 0\nsize: 1\ntotalElements:")));
	}

	@Test
	void listaVaziaNoXmlSaiComOElementoContent() throws Exception {
		mockMvc.perform(get("/alunos").param("nome", "ninguem-com-este-nome").accept("application/xml"))
				.andExpect(content().string(containsString("<content/>")));
	}

	@Test
	void dataHoraDoErroSaiPadronizadaEmMilissegundosUtc() throws Exception {
		mockMvc.perform(get("/alunos/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.dataHora").value(matchesPattern(DATA_HORA)))
				.andExpect(content().string(org.hamcrest.Matchers.startsWith("{\"dataHora\":\"")))
				.andExpect(content().string(containsString("\",\"status\":404,\"erro\":\"Não encontrado\",\"mensagem\":")));
	}

}
