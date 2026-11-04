package br.com.vitor.apicrud_alunos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

// Testes unitários da configuração de CORS: sem Spring e sem banco
class WebConfigCorsTests {

	// o getCorsConfigurations() do CorsRegistry é protegido: esta subclasse o expõe só para o teste
	private static class RegistroEspiao extends CorsRegistry {
		Map<String, CorsConfiguration> configuracoes() {
			return getCorsConfigurations();
		}
	}

	private static RegistroEspiao registrar(String... origens) {
		RegistroEspiao registro = new RegistroEspiao();
		new WebConfig(Arrays.asList(origens)).addCorsMappings(registro);
		return registro;
	}

	// ---------- validação: a aplicação não sobe com configuração perigosa ou errada ----------

	@Test
	void curingaNaoEPermitidoEmNenhumaForma() {
		for (String curinga : new String[] { "*", "https://*", "https://*.exemplo.com", "https://app.exemplo.com,*" }) {
			assertThatThrownBy(() -> new WebConfig(List.of(curinga.split(","))))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("não pode usar '*'");
		}
	}

	@Test
	void origemMalFormadaFazAAplicacaoFalharNaInicializacao() {
		for (String ruim : new String[] {
				"https://app.exemplo.com/", // barra no fim
				"https://app.exemplo.com/caminho", // com caminho
				"app.exemplo.com", // sem esquema
				"ftp://app.exemplo.com", // esquema errado
				"https://", // sem domínio
				"https://app.exemplo.com:porta", // porta que não é número
				"https://app exemplo.com", // espaço
				"javascript:alert(1)" }) {
			assertThatThrownBy(() -> new WebConfig(List.of(ruim)))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Origem de CORS inválida");
		}
	}

	@Test
	void origensValidasSaoAceitasEOsEspacosSaoAparados() {
		assertThat(WebConfig.validar(List.of(" https://app.exemplo.com ", "http://localhost:3000", "https://a.b-c.exemplo.com:8443")))
				.containsExactly("https://app.exemplo.com", "http://localhost:3000", "https://a.b-c.exemplo.com:8443");
	}

	@Test
	void entradasVaziasSaoIgnoradas() {
		assertThat(WebConfig.validar(List.of("", "   "))).isEmpty();
		assertThat(WebConfig.validar(List.of())).isEmpty();
	}

	// ---------- o que é registrado ----------

	@Test
	void semOrigensConfiguradasNaoHaCorsNenhum() {
		assertThat(registrar().configuracoes()).isEmpty();
		assertThat(registrar("", " ").configuracoes()).isEmpty();
	}

	@Test
	void comOrigensRegistraSoElasMaisOsMetodosECabecalhosDaApiSemCredenciais() {
		CorsConfiguration config = registrar("https://app.exemplo.com", "http://localhost:3000").configuracoes().get("/**");

		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).containsExactly("https://app.exemplo.com", "http://localhost:3000");
		assertThat(config.getAllowedOrigins()).doesNotContain("*");
		assertThat(config.getAllowedMethods()).containsExactlyInAnyOrder("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
		assertThat(config.getAllowedHeaders()).containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
		assertThat(config.getExposedHeaders()).containsExactly("Location");
		assertThat(config.getAllowCredentials()).isFalse();
		assertThat(config.getMaxAge()).isEqualTo(3600L);
	}

}
