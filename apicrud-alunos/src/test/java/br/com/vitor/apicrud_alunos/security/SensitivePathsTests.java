package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;

// ENDURECIMENTO: nada de "documentação", console, métricas ou arquivos de configuração exposto. Com a regra final
// denyAll, o que não tem rota declarada é negado até para o ADMIN. Filtros ligados e banco de verdade.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SensitivePathsTests {

	// caminhos clássicos de exposição acidental: console de banco, actuator, arquivos de ambiente.
	// O Swagger e o v3/api-docs ficam de FORA de propósito: o projeto ainda não tem Swagger (decisão do responsável:
	// só se valida quando ele existir).
	private static final List<String> CAMINHOS_SENSIVEIS = List.of(
			"/actuator", "/actuator/health", "/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/mappings",
			"/h2-console", "/h2-console/", "/console",
			"/.env", "/.git/config", "/application.yml", "/application.properties", "/WEB-INF/web.xml", "/server-status",
			"/error",
			"/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/alunos", "/v3/api-docs/swagger-config");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private jakarta.persistence.EntityManager em;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private JwtTokenProvider provider;

	private String tokenAdmin() {
		Permission admin = permissionRepository.findByDescription(Authorities.ADMIN).orElseThrow();
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword("{bcrypt}hash-ficticio");
		u.setPermissions(new ArrayList<>(List.of(admin)));
		em.persist(u);
		em.flush();
		em.clear();
		return provider.createTokenPair(u).accessToken();
	}

	@Test
	void caminhosSensiveisDao401ParaAnonimo() throws Exception {
		for (String caminho : CAMINHOS_SENSIVEIS) {
			mockMvc.perform(get(caminho)).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void caminhosSensiveisDao403ateParaOAdministrador() throws Exception {
		String token = tokenAdmin();
		for (String caminho : CAMINHOS_SENSIVEIS) {
			int obtido = mockMvc.perform(get(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
					.andReturn().getResponse().getStatus();
			assertThat(obtido).as("GET %s como ADMIN", caminho).isEqualTo(403);
		}
	}

	@Test
	void osMetodosPerigososEmCaminhosSensiveisTambemSaoNegados() throws Exception {
		String token = tokenAdmin();
		for (String caminho : new String[] { "/actuator/shutdown", "/actuator/env", "/h2-console" }) {
			mockMvc.perform(post(caminho).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
		}
	}

	@Test
	void osCabecalhosDeSegurancaPadraoDoSpringSecurityEstaoPresentes() throws Exception {
		mockMvc.perform(get("/alunos"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Cache-Control", containsString("no-cache")))
				.andExpect(header().doesNotExist("X-Powered-By"))
				.andExpect(header().doesNotExist("WWW-Authenticate"));
	}

	@Test
	void osErrosNuncaVazamPilhaNemNomesDeClassesNemCaminhos() throws Exception {
		String token = tokenAdmin();
		List<String> corpos = new ArrayList<>();
		corpos.add(mockMvc.perform(get("/alunos/abc").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(get("/alunos/99999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(get("/alunos").param("sort", "senha").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(post("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content("{quebrado")).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(post("/alunos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.TEXT_PLAIN).content("x")).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(get("/rota-que-nao-existe").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(get("/alunos")).andReturn().getResponse().getContentAsString());
		corpos.add(mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"x\",\"password\":\"y\"}")).andReturn().getResponse().getContentAsString());

		for (String corpo : corpos) {
			assertThat(corpo).isNotBlank()
					.doesNotContain("Exception").doesNotContain("br.com.vitor").doesNotContain("org.springframework")
					.doesNotContain("com.fasterxml").doesNotContain("tools.jackson").doesNotContain("\tat ")
					.doesNotContain("stackTrace").doesNotContain("trace").doesNotContain("C:\\\\").doesNotContain("/home/");
		}
	}

}
