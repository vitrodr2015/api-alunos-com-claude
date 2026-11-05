package br.com.vitor.apicrud_alunos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// "BANCO CAÍDO" (por exemplo, RDS parado): o /actuator/health completo responde 503 DOWN, mas o liveness continua UP.
// Por isso o orquestrador (ECS, ALB) deve apontar para /actuator/health/liveness: senão reiniciaria a tarefa em laço
// enquanto o banco estivesse fora. Aqui um componente falso simula o banco fora do ar; nada vaza no corpo.
@SpringBootTest
@AutoConfigureMockMvc
@Import(HealthDownTests.BancoForaDoAr.class)
class HealthDownTests {

	@TestConfiguration
	static class BancoForaDoAr {
		@Bean
		HealthIndicator banco() {
			return () -> Health.down().withDetail("erro", "Connection refused: jdbc:mysql://segredo:3306/x").build();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthCompletoDa503DOWNSemVazarDetalhes() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("DOWN")).andExpect(jsonPath("$.components").doesNotExist())
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("jdbc"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
	}

	@Test
	void livenessContinuaUPEReadinessTambemMesmoComOBancoFora() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\"}", true));
		mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
				.andExpect(content().json("{\"status\":\"UP\"}", true));
	}

}
