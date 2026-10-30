package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ContentNegotiationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void semAcceptRespondeEmJson() throws Exception {
		mockMvc.perform(get("/alunos/1"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.nome").value("Ana Souza"));
	}

	@Test
	void acceptJsonRespondeEmJson() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/json"))
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.dataNascimento").value("2000-05-10"));
	}

	@Test
	void acceptQualquerCoisaRespondeEmJson() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("*/*"))
				.andExpect(header().string("Content-Type", containsString("application/json")));
	}

	@Test
	void acceptXmlRespondeEmXmlComDataLegivel() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/xml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/xml")))
				.andExpect(content().string(containsString("<nome>Ana Souza</nome>")))
				.andExpect(content().string(containsString("<dataNascimento>2000-05-10</dataNascimento>")));
	}

	@Test
	void acceptYamlRespondeEmYamlComDataLegivel() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/yaml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/yaml")))
				.andExpect(content().string(containsString("nome: \"Ana Souza\"")))
				.andExpect(content().string(containsString("dataNascimento: \"2000-05-10\"")));
	}

	@Test
	void acceptXYamlRespondeEmYaml() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/x-yaml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/x-yaml")))
				.andExpect(content().string(containsString("nome: \"Ana Souza\"")));
	}

	@Test
	void listaPaginadaTambemRespondeNosTresFormatos() throws Exception {
		mockMvc.perform(get("/alunos").accept("application/json"))
				.andExpect(jsonPath("$.content[0].nome").value("Ana Souza"))
				.andExpect(jsonPath("$.totalElements").isNumber());
		mockMvc.perform(get("/alunos").accept("application/xml"))
				.andExpect(content().string(containsString("<totalElements>")))
				.andExpect(content().string(containsString("<nome>Ana Souza</nome>")));
		mockMvc.perform(get("/alunos").accept("application/yaml"))
				.andExpect(content().string(containsString("totalElements:")))
				.andExpect(content().string(containsString("nome: \"Ana Souza\"")));
	}

	@Test
	void datasNaoSaemComoNumeros() throws Exception {
		for (String formato : new String[] { "application/json", "application/xml", "application/yaml" }) {
			mockMvc.perform(get("/alunos/1").accept(formato))
					.andExpect(content().string(not(containsString("[2000,"))))
					.andExpect(content().string(containsString("2000-05-10")));
		}
	}

	@Test
	void corpoDaRequisicaoTambemPodeSerXmlOuYaml() throws Exception {
		String xml = "<AlunoRequest><nome>Fabio Xml</nome><email>" + emailUnico()
				+ "</email><dataNascimento>1995-02-03</dataNascimento></AlunoRequest>";
		mockMvc.perform(post("/alunos").contentType("application/xml").accept("application/xml").content(xml))
				.andExpect(status().isCreated())
				.andExpect(content().string(containsString("<nome>Fabio Xml</nome>")));

		String yaml = "nome: Gina Yaml\nemail: " + emailUnico() + "\ndataNascimento: 1996-04-05\n";
		mockMvc.perform(post("/alunos").contentType("application/yaml").accept("application/yaml").content(yaml))
				.andExpect(status().isCreated())
				.andExpect(content().string(containsString("nome: \"Gina Yaml\"")));
	}

	@Test
	void erroContinuaEmJsonMesmoPedindoXml() throws Exception {
		mockMvc.perform(get("/alunos/999999").accept("application/xml"))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.status").value(404));
	}

	private static String emailUnico() {
		return "formato-" + UUID.randomUUID() + "@escola.com";
	}

}
