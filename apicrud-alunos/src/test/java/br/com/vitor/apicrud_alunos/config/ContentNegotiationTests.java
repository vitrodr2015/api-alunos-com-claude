package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// Desde o passo do HATEOAS, a listagem e o GET por id respondem só em HAL (JSON com _links);
// XML e YAML continuam valendo nos demais endpoints. O PATCH /alunos/1/enable é idempotente
// (a Ana Souza já é ativa) e devolve um AlunoResponse comum, então serve para provar os três formatos.
@SpringBootTest
@AutoConfigureMockMvc
class ContentNegotiationTests {

	@Autowired
	private MockMvc mockMvc;

	// ---------- GET: HAL (JSON) ----------

	@Test
	void semAcceptRespondeEmJsonHal() throws Exception {
		mockMvc.perform(get("/alunos/1"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$._links.self.href").exists());
	}

	@Test
	void acceptHalJsonRespondeComOTipoHal() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/hal+json"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/hal+json")))
				.andExpect(jsonPath("$._links.alunos.href").exists());
	}

	@Test
	void acceptJsonRespondeEmJsonComDataLegivel() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("application/json"))
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.dataNascimento").value("2000-05-10"));
	}

	@Test
	void acceptQualquerCoisaRespondeEmJson() throws Exception {
		mockMvc.perform(get("/alunos/1").accept("*/*"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/json")));
	}

	@Test
	void getPorIdEListagemEmXmlOuYamlSaoRecusadosComErroEmJson() throws Exception {
		for (String formato : new String[] { "application/xml", "application/yaml", "application/x-yaml" }) {
			for (String rota : new String[] { "/alunos/1", "/alunos" }) {
				mockMvc.perform(get(rota).accept(formato))
						.andExpect(status().isNotAcceptable())
						.andExpect(header().string("Content-Type", containsString("application/json")))
						.andExpect(jsonPath("$.status").value(406));
			}
		}
	}

	// ---------- demais endpoints: JSON, XML e YAML ----------

	@Test
	void patchAcceptJsonRespondeEmJsonSemLinks() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$._links").doesNotExist());
	}

	@Test
	void patchAcceptXmlRespondeEmXmlComDataLegivel() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/xml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/xml")))
				.andExpect(content().string(containsString("<nome>Ana Souza</nome>")))
				.andExpect(content().string(containsString("<dataNascimento>2000-05-10</dataNascimento>")));
	}

	@Test
	void patchAcceptYamlRespondeEmYamlComDataLegivel() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/yaml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/yaml")))
				.andExpect(content().string(containsString("nome: \"Ana Souza\"")))
				.andExpect(content().string(containsString("dataNascimento: \"2000-05-10\"")));
	}

	@Test
	void patchAcceptXYamlRespondeEmYaml() throws Exception {
		mockMvc.perform(patch("/alunos/1/enable").accept("application/x-yaml"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", containsString("application/x-yaml")))
				.andExpect(content().string(containsString("nome: \"Ana Souza\"")));
	}

	@Test
	void datasNaoSaemComoNumerosEmNenhumFormato() throws Exception {
		// GET em HAL (JSON) e PATCH nos três formatos
		mockMvc.perform(get("/alunos/1").accept("application/hal+json"))
				.andExpect(content().string(not(containsString("[2000,"))))
				.andExpect(content().string(containsString("2000-05-10")));
		for (String formato : new String[] { "application/json", "application/xml", "application/yaml" }) {
			mockMvc.perform(patch("/alunos/1/enable").accept(formato))
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
		String corpo = "{\"nome\":\"Fulano Teste\",\"email\":\"" + emailUnico() + "\",\"dataNascimento\":\"2000-01-01\"}";
		mockMvc.perform(put("/alunos/999999").contentType("application/json").accept("application/xml").content(corpo))
				.andExpect(status().isNotFound())
				.andExpect(header().string("Content-Type", containsString("application/json")))
				.andExpect(jsonPath("$.status").value(404));
	}

	private static String emailUnico() {
		return "formato-" + UUID.randomUUID() + "@escola.com";
	}

}
