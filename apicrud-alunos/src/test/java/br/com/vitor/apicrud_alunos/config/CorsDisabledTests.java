package br.com.vitor.apicrud_alunos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

// O padrão da aplicação: NENHUMA origem configurada. Nenhum navegador de outro domínio é autorizado.
@SpringBootTest(properties = "cors.allowed-origins=")
@AutoConfigureMockMvc
class CorsDisabledTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void semOrigensConfiguradasNenhumaOrigemRecebeCabecalhoDeCors() throws Exception {
		for (String origem : new String[] { "https://app.exemplo.com", "http://localhost:3000", "https://site-malicioso.com" }) {
			mockMvc.perform(options("/alunos").header(HttpHeaders.ORIGIN, origem)
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
					.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
					.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
					.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
					.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
		}
	}

	@Test
	void umaRequisicaoComOrigemNaoGanhaCabecalhoDeCorsEContinuaProtegida() throws Exception {
		mockMvc.perform(get("/alunos").header(HttpHeaders.ORIGIN, "https://app.exemplo.com"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
				.andExpect(jsonPath("$.erro").value("Não autenticado"));
	}

}
