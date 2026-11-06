package br.com.vitor.apicrud_alunos.config;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// HEALTH CHECK (Actuator), com os filtros de segurança LIGADOS e sem token: é público, só de leitura e só devolve o status.
// Os outros endpoints do Actuator continuam negados (SensitivePathsTests). O caso "banco caído" está em HealthDownTests.
@SpringBootTest
@AutoConfigureMockMvc
class HealthEndpointTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEPublicoDevolveUPEnadaMais() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				// sem detalhes: nada de banco, disco, componentes nem versões
				.andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(jsonPath("$.details").doesNotExist())
				// com os probes ligados vem também o NOME dos grupos (liveness e readiness): não é dado sensível
				.andExpect(jsonPath("$.groups", containsInAnyOrder("liveness", "readiness")));
	}

	@Test
	void livenessEReadinessSaoPublicosEDevolvemUP() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\"}", true));
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\"}", true));
	}

	@Test
	void healthNaoPrecisaDeTokenMasUmTokenInvalidoNaoQuebraNada() throws Exception {
		// rota pública: o filtro do JWT não pode barrar quem manda um token ruim (ou nenhum)
		mockMvc.perform(get("/actuator/health").header("Authorization", "Bearer isto-nao-e-um-token"))
				.andExpect(status().isOk());
	}

	@Test
	void soOGetEPublico() throws Exception {
		mockMvc.perform(post("/actuator/health")).andExpect(status().isUnauthorized());
		mockMvc.perform(put("/actuator/health")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/actuator/health")).andExpect(status().isUnauthorized());
	}

	@Test
	void aRaizDoActuatorENenhumOutroEndpointSaoPublicos() throws Exception {
		mockMvc.perform(get("/actuator")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
		// o firewall do Spring Security rejeita o ".." (400): também é bloqueio
		mockMvc.perform(get("/actuator/health/../env")).andExpect(status().isBadRequest());
	}

}
