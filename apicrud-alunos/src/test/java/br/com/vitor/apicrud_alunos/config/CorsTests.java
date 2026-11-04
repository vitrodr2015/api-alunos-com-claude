package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.security.Authorities;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;

// CORS com a cadeia de segurança LIGADA (é o que importa: o preflight não leva token e precisa passar pela segurança).
// Origens permitidas nos testes (application.yml de teste): https://app.exemplo.com e http://localhost:3000
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CorsTests {

	private static final String PERMITIDA = "https://app.exemplo.com";
	private static final String OUTRA_PERMITIDA = "http://localhost:3000";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider provider;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private EntityManager em;

	private String tokenUser() {
		Permission user = permissionRepository.findByDescription(Authorities.USER).orElseThrow();
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword("{bcrypt}hash-ficticio");
		u.setPermissions(new ArrayList<>(List.of(user)));
		em.persist(u);
		em.flush();
		em.clear();
		return provider.createTokenPair(u).accessToken();
	}

	private ResultActions preflight(String origem, String metodo, String cabecalhos) throws Exception {
		var req = options("/alunos").header(HttpHeaders.ORIGIN, origem).header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodo);
		if (cabecalhos != null) {
			req.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, cabecalhos);
		}
		return mockMvc.perform(req);
	}

	// ======================== preflight (OPTIONS) ========================

	@Test
	void preflightDeOrigemPermitidaPassaSemTokenEDevolveOsCabecalhosDeCors() throws Exception {
		// o preflight NUNCA leva token; se a segurança o barrasse (401), o navegador reclamaria de CORS
		preflight(PERMITIDA, "POST", "authorization,content-type")
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDA))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
				// o Spring devolve a lista em minúsculas e com espaço depois da vírgula
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, equalToIgnoringCase("authorization, content-type")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void aOrigemDevolvidaEsempreAPedidaNuncaUmCuringa() throws Exception {
		preflight(OUTRA_PERMITIDA, "GET", null)
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OUTRA_PERMITIDA))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, not("*")));
	}

	@Test
	void todosOsMetodosDaApiSaoPermitidosNoPreflight() throws Exception {
		for (String metodo : new String[] { "GET", "POST", "PUT", "PATCH", "DELETE" }) {
			preflight(PERMITIDA, metodo, "authorization").andExpect(status().isOk());
		}
	}

	@Test
	void preflightDeOrigemNaoPermitidaEBarradoSemCabecalhoDeCors() throws Exception {
		preflight("https://site-malicioso.com", "GET", "authorization")
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void origemParecidaMasDiferenteEBarrada() throws Exception {
		for (String parecida : new String[] {
				"https://app.exemplo.com.site-malicioso.com", // o domínio permitido como começo do domínio do atacante
				"https://x.app.exemplo.com", // subdomínio não listado
				"http://app.exemplo.com", // esquema diferente
				"https://app.exemplo.com:8443", // porta diferente
				"https://APP.EXEMPLO.COM.evil.com",
				"null" }) {
			preflight(parecida, "GET", "authorization")
					.andExpect(status().isForbidden())
					.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		}
	}

	@Test
	void metodoForaDaListaEBarradoNoPreflight() throws Exception {
		preflight(PERMITIDA, "TRACE", "authorization").andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void cabecalhoForaDaListaEBarradoNoPreflight() throws Exception {
		preflight(PERMITIDA, "GET", "x-cabecalho-qualquer").andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void seOPreflightMisturaCabecalhoPermitidoEProibidoSoOPermitidoVoltaNaLista() throws Exception {
		// comportamento do Spring: responde 200, mas devolve em Allow-Headers APENAS os cabeçalhos permitidos.
		// Quem barra é o navegador: ele pediu "x-outro", não o viu liberado e não envia a requisição de verdade.
		preflight(PERMITIDA, "GET", "authorization,x-outro")
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, equalToIgnoringCase("authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, not(containsString("x-outro"))));
	}

	// ======================== requisições de verdade ========================

	@Test
	void requisicaoDeOrigemPermitidaComTokenRecebeOsCabecalhosDeCors() throws Exception {
		mockMvc.perform(get("/alunos").header(HttpHeaders.ORIGIN, PERMITIDA)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser()))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDA))
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void oLocationDoPostEExpostoParaOJavaScriptDoNavegador() throws Exception {
		String corpo = "{\"nome\":\"Aluno Cors\",\"email\":\"cors-" + UUID.randomUUID() + "@escola.com\",\"dataNascimento\":\"2000-01-01\"}";

		mockMvc.perform(post("/alunos").header(HttpHeaders.ORIGIN, PERMITIDA)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser())
				.contentType(MediaType.APPLICATION_JSON).content(corpo))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDA))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")))
				.andExpect(header().exists(HttpHeaders.LOCATION));
	}

	@Test
	void requisicaoDeOrigemNaoPermitidaEBarradaMesmoComTokenValido() throws Exception {
		mockMvc.perform(get("/alunos").header(HttpHeaders.ORIGIN, "https://site-malicioso.com")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser()))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.andExpect(content().string(not(containsString("_embedded"))));
	}

	@Test
	void oErro401DeUmaOrigemPermitidaTambemTrazCorsParaONavegadorConseguirLerAMensagem() throws Exception {
		// sem token: 401 em JSON. Sem o cabeçalho de CORS, o navegador esconderia esse 401 e mostraria só "erro de CORS"
		mockMvc.perform(get("/alunos").header(HttpHeaders.ORIGIN, PERMITIDA))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, PERMITIDA))
				.andExpect(jsonPath("$.erro").value("Não autenticado"));
	}

	@Test
	void corsNaoAbreNenhumaRotaOPreflightPassaMasAChamadaContinuaProtegida() throws Exception {
		preflight(PERMITIDA, "DELETE", "authorization").andExpect(status().isOk());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/alunos/1")
				.header(HttpHeaders.ORIGIN, PERMITIDA)).andExpect(status().isUnauthorized());
	}

	@Test
	void requisicaoSemOrigemNaoGanhaCabecalhosDeCors() throws Exception {
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser()))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
