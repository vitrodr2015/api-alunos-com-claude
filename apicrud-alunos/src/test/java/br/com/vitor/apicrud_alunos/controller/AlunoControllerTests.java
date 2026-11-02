package br.com.vitor.apicrud_alunos.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
		mockMvc.perform(get("/alunos/7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Gustavo Alves"))
				.andExpect(jsonPath("$.email").value("gustavo.alves@escola.com"));
		mockMvc.perform(get("/alunos/8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Helena Barros"))
				.andExpect(jsonPath("$.email").value("helena.barros@escola.com"));
		mockMvc.perform(get("/alunos").param("nome", "souza"))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(1));
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
				.andExpect(jsonPath("$._embedded.alunos.length()").value(2))
				.andExpect(jsonPath("$._embedded.alunos[0].nome").value(marca + " 1"))
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$.page.totalElements").value(3))
				.andExpect(jsonPath("$.page.totalPages").value(2));

		mockMvc.perform(get("/alunos").param("nome", marca.toLowerCase()).param("size", "2").param("page", "1")
				.param("sort", "nome,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.alunos.length()").value(1))
				.andExpect(jsonPath("$._embedded.alunos[0].nome").value(marca + " 1"));
	}

	@Test
	void listarSemResultadosRetorna200ComListaVazia() throws Exception {
		mockMvc.perform(get("/alunos").param("nome", "nome-que-ninguem-tem-" + UUID.randomUUID()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded").doesNotExist())
				.andExpect(jsonPath("$.page.totalElements").value(0));
	}

	@Test
	void listarFiltraPorAtivo() throws Exception {
		String marca = "Filtro" + UUID.randomUUID().toString().substring(0, 8);
		long ativo = criar(marca + " A", emailUnico());
		long inativo = criar(marca + " B", emailUnico());
		mockMvc.perform(patch("/alunos/" + inativo + "/disable")).andExpect(status().isOk());

		mockMvc.perform(get("/alunos").param("nome", marca).param("ativo", "true"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(ativo));
		mockMvc.perform(get("/alunos").param("nome", marca).param("ativo", "false"))
				.andExpect(jsonPath("$.page.totalElements").value(1))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(inativo));
		mockMvc.perform(get("/alunos").param("nome", marca))
				.andExpect(jsonPath("$.page.totalElements").value(2));
	}

	@Test
	void listarOrdenandoPorCampoInexistenteRetorna400() throws Exception {
		mockMvc.perform(get("/alunos").param("sort", "senha")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erro").value("Requisição inválida"))
				.andExpect(jsonPath("$.mensagem").value(containsString("'senha'")));
		mockMvc.perform(get("/alunos").param("sort", "nome,sideways")).andExpect(status().isBadRequest());
	}

	@Test
	void listarComFiltroAtivoInvalidoRetorna400() throws Exception {
		mockMvc.perform(get("/alunos").param("ativo", "talvez")).andExpect(status().isBadRequest());
	}

	@Test
	void listarSemParametrosUsaPaginaZeroComTamanhoPadraoVinteOrdenadaPeloId() throws Exception {
		mockMvc.perform(get("/alunos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.number").value(0))
				.andExpect(jsonPath("$.page.size").value(20))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(1))
				.andExpect(jsonPath("$._embedded.alunos[1].id").value(2));
	}

	@Test
	void listarComSizeAcimaDoMaximoReduzParaCinquenta() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "500"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(50));
	}

	@Test
	void listarComPageESizeForaDaFaixaCaiNosPadroesEmVezDeDar400() throws Exception {
		mockMvc.perform(get("/alunos").param("page", "-1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.number").value(0));
		mockMvc.perform(get("/alunos").param("size", "0")).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(20));
		mockMvc.perform(get("/alunos").param("page", "abc")).andExpect(status().isOk())
				.andExpect(jsonPath("$.page.number").value(0));
	}

	@Test
	void listarOrdenaPorQualquerCampoDaEntidadeEmOrdemDecrescente() throws Exception {
		mockMvc.perform(get("/alunos").param("sort", "dataNascimento,desc").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(2))
				.andExpect(jsonPath("$._embedded.alunos.length()").value(2));
		mockMvc.perform(get("/alunos").param("sort", "ativo")).andExpect(status().isOk());
	}

	// ---------- paginação com volume (os 400 alunos do data.sql, ids 9 a 408) ----------
	// Outros testes criam e desativam alunos no mesmo banco, então os totais são comparados com "pelo menos".

	@Test
	void paginacaoComVolumeCalculaTotalEPaginas() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded.alunos.length()").value(20))
				.andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(408)))
				.andExpect(jsonPath("$.page.totalPages").value(greaterThanOrEqualTo(21)));
	}

	@Test
	void paginasSeguidasSaoSequenciaisSemRepetirNemPularAlunos() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "50").param("page", "0"))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(1))
				.andExpect(jsonPath("$._embedded.alunos[49].id").value(50));
		mockMvc.perform(get("/alunos").param("size", "50").param("page", "1"))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(51))
				.andExpect(jsonPath("$._embedded.alunos[49].id").value(100));
		mockMvc.perform(get("/alunos").param("size", "50").param("page", "8"))
				.andExpect(jsonPath("$._embedded.alunos[0].id").value(401));
	}

	@Test
	void sizeAcimaDoMaximoDevolveNoMaximoCinquentaItens() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "500"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(50))
				.andExpect(jsonPath("$._embedded.alunos.length()").value(50));
	}

	@Test
	void paginaAlemDoFimDevolveListaVaziaComOTotalCorreto() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "50").param("page", "1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._embedded").doesNotExist())
				.andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(408)));
	}

	@Test
	void filtroPorAtivoFalsoPaginaSoOsInativosDosDadosDeTeste() throws Exception {
		// 1 em cada 10 dos 400 alunos de teste é inativo (40); outros testes podem desativar mais
		mockMvc.perform(get("/alunos").param("ativo", "false").param("size", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(40)))
				.andExpect(jsonPath("$._embedded.alunos[0].ativo").value(false));
	}

	@Test
	void filtroPorNomeEncontraUmNomeGeradoEPagina() throws Exception {
		mockMvc.perform(get("/alunos").param("nome", "ADRIANA").param("sort", "nome,asc").param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(5))
				.andExpect(jsonPath("$._embedded.alunos[0].nome").value(containsString("Adriana")))
				.andExpect(jsonPath("$.page.totalElements").value(greaterThanOrEqualTo(20)));
	}

	// ---------- HATEOAS (HAL) na listagem e no GET por id ----------

	@Test
	void listagemTrazLinksDePaginaEDeCadaAluno() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "10").param("page", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.self.href").value(containsString("page=2")))
				.andExpect(jsonPath("$._links.first.href").value(containsString("page=0")))
				.andExpect(jsonPath("$._links.prev.href").value(containsString("page=1")))
				.andExpect(jsonPath("$._links.next.href").value(containsString("page=3")))
				.andExpect(jsonPath("$._links.last.href").exists())
				.andExpect(jsonPath("$.page.number").value(2))
				.andExpect(jsonPath("$._embedded.alunos[0]._links.self.href").value(containsString("/alunos/")))
				.andExpect(jsonPath("$._embedded.alunos[0]._links.alunos.href").exists());
	}

	@Test
	void primeiraPaginaNaoTemPrevEUltimaNaoTemNext() throws Exception {
		mockMvc.perform(get("/alunos").param("size", "50"))
				.andExpect(jsonPath("$._links.prev").doesNotExist())
				.andExpect(jsonPath("$._links.next.href").exists());
		mockMvc.perform(get("/alunos").param("size", "50").param("page", "1000"))
				.andExpect(jsonPath("$._links.next").doesNotExist());
	}

	@Test
	void linksDaPaginaPreservamOsFiltrosEOTamanho() throws Exception {
		// a query string precisa estar na URL (o MockMvc não a monta a partir de .param()), pois os links nascem da URL
		mockMvc.perform(get("/alunos?nome=adriana&ativo=true&size=2"))
				.andExpect(jsonPath("$._links.next.href").value(containsString("nome=adriana")))
				.andExpect(jsonPath("$._links.next.href").value(containsString("ativo=true")))
				.andExpect(jsonPath("$._links.next.href").value(containsString("size=2")));
	}

	@Test
	void alunoAtivoOfereceDisableEInativoOfereceEnable() throws Exception {
		String email = emailUnico();
		long id = criar("Link Estado " + UUID.randomUUID().toString().substring(0, 8), email);

		mockMvc.perform(get("/alunos/" + id))
				.andExpect(jsonPath("$._links.self.href").value(containsString("/alunos/" + id)))
				.andExpect(jsonPath("$._links.disable.href").value(containsString("/alunos/" + id + "/disable")))
				.andExpect(jsonPath("$._links.enable").doesNotExist());

		mockMvc.perform(patch("/alunos/" + id + "/disable")).andExpect(status().isOk());
		mockMvc.perform(get("/alunos/" + id))
				.andExpect(jsonPath("$._links.enable.href").value(containsString("/alunos/" + id + "/enable")))
				.andExpect(jsonPath("$._links.disable").doesNotExist());
	}

	@Test
	void endpointsDeEscritaNaoTrazemLinks() throws Exception {
		String email = emailUnico();
		mockMvc.perform(post("/alunos").contentType(MediaType.APPLICATION_JSON)
				.content(corpo("Sem Links Escrita", email, "2000-01-01")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$._links").doesNotExist())
				.andExpect(jsonPath("$.nome").value("Sem Links Escrita"));
		mockMvc.perform(patch("/alunos/1/enable"))
				.andExpect(jsonPath("$._links").doesNotExist());
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
