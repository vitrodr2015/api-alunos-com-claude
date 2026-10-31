package br.com.vitor.apicrud_alunos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class AlunoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	// ---------- registros fixos ----------

	@Test
	void registrosFixosExistemAoSubirAAplicacao() throws Exception {
		mockMvc.perform(get("/alunos/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.email").value("ana.souza@escola.com"))
				.andExpect(jsonPath("$.dataNascimento").value("2000-05-10"))
				.andExpect(jsonPath("$.ativo").value(true));
		mockMvc.perform(get("/alunos/5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Eduarda Costa"));
		mockMvc.perform(get("/alunos/6"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Felipe Rocha"))
				.andExpect(jsonPath("$.email").value("felipe.rocha@escola.com"));
		mockMvc.perform(get("/alunos").param("nome", "souza"))
				.andExpect(jsonPath("$.content[0].id").value(1));
	}

	@Test
	void emailDeRegistroFixoJaEstaEmUso() throws Exception {
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Outra Ana", "ana.souza@escola.com", "2000-05-10")))
				.andExpect(status().isConflict());
	}

	// ---------- POST /alunos ----------

	@Test
	void criarRetorna201ComLocation() throws Exception {
		String email = emailUnico();
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON).content(corpo("Ana Souza", email, "2000-05-10")))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", containsString("/alunos/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("Ana Souza"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.dataNascimento").value("2000-05-10"))
				.andExpect(jsonPath("$.ativo").value(true));
	}

	@Test
	void criarIgnoraIdEnviadoNoCorpo() throws Exception {
		String json = "{\"id\":999,\"nome\":\"Bia Lima\",\"email\":\"" + emailUnico() + "\",\"dataNascimento\":\"1999-01-01\"}";
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(999)));
	}

	@Test
	void criarComCamposInvalidosRetorna400ListandoOsCampos() throws Exception {
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Al\",\"email\":\"nao-e-email\",\"dataNascimento\":\"2999-01-01\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value(containsString("nome: deve ter de 3 a 100 caracteres")))
				.andExpect(jsonPath("$.mensagem").value(containsString("email: deve ser um e-mail válido")))
				.andExpect(jsonPath("$.mensagem").value(containsString("dataNascimento: deve ser uma data no passado")));
	}

	@Test
	void criarSemCamposObrigatoriosRetorna400() throws Exception {
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("nome: é obrigatório")))
				.andExpect(jsonPath("$.mensagem").value(containsString("email: é obrigatório")))
				.andExpect(jsonPath("$.mensagem").value(containsString("dataNascimento: é obrigatória")));
	}

	@Test
	void criarComDataEmFormatoInvalidoRetorna400() throws Exception {
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Caio Alves", emailUnico(), "10/05/2000")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void criarComEmailRepetidoRetorna409IgnorandoMaiusculas() throws Exception {
		String email = emailUnico();
		criar("Dani Rocha", email);
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Outro Nome", email.toUpperCase(), "2001-02-03")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409));
	}

	// ---------- GET /alunos/{id} ----------

	@Test
	void buscarRetorna200() throws Exception {
		long id = criar("Eva Costa", emailUnico());
		mockMvc.perform(get("/alunos/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Eva Costa"));
	}

	@Test
	void buscarInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/alunos/987654"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void idInvalidoRetorna400() throws Exception {
		for (String id : new String[] { "abc", "0", "-5" }) {
			mockMvc.perform(get("/alunos/" + id)).andExpect(status().isBadRequest());
			mockMvc.perform(delete("/alunos/" + id)).andExpect(status().isBadRequest());
			mockMvc.perform(patch("/alunos/" + id + "/disable")).andExpect(status().isBadRequest());
			mockMvc.perform(patch("/alunos/" + id + "/enable")).andExpect(status().isBadRequest());
			mockMvc.perform(put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON)
					.content(corpo("Fabio Dias", emailUnico(), "2000-01-01")))
					.andExpect(status().isBadRequest());
		}
	}

	// ---------- GET /alunos ----------

	@Test
	void listarPagina() throws Exception {
		String marca = "Listagem" + UUID.randomUUID().toString().substring(0, 8);
		for (int i = 1; i <= 3; i++) {
			criar(marca + " " + i, emailUnico());
		}
		mockMvc.perform(get("/alunos").param("nome", marca).param("size", "2").param("sort", "nome"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].nome").value(marca + " 1"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/alunos").param("nome", marca.toLowerCase()).param("size", "2").param("page", "1")
				.param("sort", "nome,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].nome").value(marca + " 1"));
	}

	@Test
	void listarSemResultadosRetorna200ComListaVazia() throws Exception {
		mockMvc.perform(get("/alunos").param("nome", "nome-que-ninguem-tem-" + UUID.randomUUID()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0))
				.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void listarFiltraPorAtivo() throws Exception {
		String marca = "Filtro" + UUID.randomUUID().toString().substring(0, 8);
		long ativo = criar(marca + " A", emailUnico());
		long inativo = criar(marca + " B", emailUnico());
		mockMvc.perform(patch("/alunos/" + inativo + "/disable")).andExpect(status().isOk());

		mockMvc.perform(get("/alunos").param("nome", marca).param("ativo", "true"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(ativo));
		mockMvc.perform(get("/alunos").param("nome", marca).param("ativo", "false"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(inativo));
		mockMvc.perform(get("/alunos").param("nome", marca))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void listarComParametrosInvalidosRetorna400() throws Exception {
		mockMvc.perform(get("/alunos").param("sort", "senha")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensagem").value(containsString("Campos aceitos")));
		mockMvc.perform(get("/alunos").param("sort", "nome,sideways")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/alunos").param("page", "-1")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/alunos").param("size", "0")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/alunos").param("size", "101")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/alunos").param("page", "abc")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/alunos").param("ativo", "talvez")).andExpect(status().isBadRequest());
	}

	// ---------- PUT /alunos/{id} ----------

	@Test
	void atualizarRetorna200ESubstituiOsCampos() throws Exception {
		String emailNovo = emailUnico();
		long id = criar("Gabi Nunes", emailUnico());
		mockMvc.perform(patch("/alunos/" + id + "/disable")).andExpect(status().isOk());

		mockMvc.perform(put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Gabriela Nunes", emailNovo, "1998-12-31")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.nome").value("Gabriela Nunes"))
				.andExpect(jsonPath("$.email").value(emailNovo))
				.andExpect(jsonPath("$.dataNascimento").value("1998-12-31"))
				.andExpect(jsonPath("$.ativo").value(false));
	}

	@Test
	void atualizarMantendoOProprioEmailRetorna200() throws Exception {
		String email = emailUnico();
		long id = criar("Hugo Reis", email);
		mockMvc.perform(put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Hugo Reis Filho", email, "1990-06-15")))
				.andExpect(status().isOk());
	}

	@Test
	void atualizarComEmailDeOutroAlunoRetorna409() throws Exception {
		String emailDoOutro = emailUnico();
		criar("Iara Melo", emailDoOutro);
		long id = criar("Jose Pires", emailUnico());
		mockMvc.perform(put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Jose Pires", emailDoOutro, "1990-06-15")))
				.andExpect(status().isConflict());
	}

	@Test
	void atualizarInexistenteRetorna404() throws Exception {
		mockMvc.perform(put("/alunos/987654").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Kleber Dias", emailUnico(), "1990-06-15")))
				.andExpect(status().isNotFound());
	}

	@Test
	void atualizarComCorpoInvalidoRetorna400() throws Exception {
		long id = criar("Lia Prado", emailUnico());
		mockMvc.perform(put("/alunos/" + id).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	// ---------- PATCH disable / enable ----------

	@Test
	void desativarEReativarSaoIdempotentes() throws Exception {
		long id = criar("Marcos Vaz", emailUnico());

		for (int i = 0; i < 2; i++) {
			mockMvc.perform(patch("/alunos/" + id + "/disable"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ativo").value(false));
		}
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(patch("/alunos/" + id + "/enable"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.ativo").value(true));
		}
	}

	@Test
	void desativarEReativarInexistenteRetorna404() throws Exception {
		mockMvc.perform(patch("/alunos/987654/disable")).andExpect(status().isNotFound());
		mockMvc.perform(patch("/alunos/987654/enable")).andExpect(status().isNotFound());
	}

	// ---------- DELETE /alunos/{id} ----------

	@Test
	void removerRetorna204ESemCorpo() throws Exception {
		long id = criar("Nina Torres", emailUnico());
		mockMvc.perform(delete("/alunos/" + id))
				.andExpect(status().isNoContent())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
		mockMvc.perform(get("/alunos/" + id)).andExpect(status().isNotFound());
		mockMvc.perform(delete("/alunos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void removerLiberaOEmailParaNovoCadastro() throws Exception {
		String email = emailUnico();
		long id = criar("Otavio Lemos", email);
		mockMvc.perform(delete("/alunos/" + id)).andExpect(status().isNoContent());
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Otavio Lemos", email, "1990-01-01")))
				.andExpect(status().isCreated());
	}

	// ---------- auxiliares ----------

	private long criar(String nome, String email) throws Exception {
		String resposta = mockMvc
				.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON).content(corpo(nome, email, "2000-01-01")))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(resposta, "$.id")).longValue();
	}

	private static String corpo(String nome, String email, String dataNascimento) {
		return "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"dataNascimento\":\"" + dataNascimento + "\"}";
	}

	private static String emailUnico() {
		return "aluno-" + UUID.randomUUID() + "@escola.com";
	}

}
