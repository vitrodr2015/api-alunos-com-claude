package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

// Testes unitários dos erros da cadeia de segurança: sem Spring e sem banco
class SecurityErrorHandlersTests {

	private final ObjectMapper mapper = JsonMapper.builder().build();
	private final SecurityErrorWriter writer = new SecurityErrorWriter(mapper);

	@Test
	void entryPointResponde401EmJsonNoFormatoErrorResponse() throws IOException {
		MockHttpServletResponse resposta = new MockHttpServletResponse();

		new RestAuthenticationEntryPoint(writer).commence(new MockHttpServletRequest(), resposta,
				new BadCredentialsException("detalhe interno que não pode vazar"));

		assertThat(resposta.getStatus()).isEqualTo(401);
		assertThat(resposta.getContentType()).startsWith("application/json");
		JsonNode corpo = mapper.readTree(resposta.getContentAsString());
		assertThat(corpo.get("status").asInt()).isEqualTo(401);
		assertThat(corpo.get("erro").asString()).isEqualTo("Não autenticado");
		assertThat(corpo.get("mensagem").asString()).contains("Authorization").contains("Bearer");
		assertThat(corpo.get("dataHora").asString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
		assertThat(resposta.getContentAsString()).doesNotContain("detalhe interno");
	}

	@Test
	void accessDeniedHandlerResponde403EmJsonNoFormatoErrorResponse() throws IOException {
		MockHttpServletResponse resposta = new MockHttpServletResponse();

		new RestAccessDeniedHandler(writer).handle(new MockHttpServletRequest(), resposta,
				new AccessDeniedException("detalhe interno que não pode vazar"));

		assertThat(resposta.getStatus()).isEqualTo(403);
		assertThat(resposta.getContentType()).startsWith("application/json");
		JsonNode corpo = mapper.readTree(resposta.getContentAsString());
		assertThat(corpo.get("status").asInt()).isEqualTo(403);
		assertThat(corpo.get("erro").asString()).isEqualTo("Acesso negado");
		assertThat(corpo.get("mensagem").asString()).isEqualTo("Você não tem permissão para acessar este recurso");
		assertThat(corpo.get("dataHora")).isNotNull();
		assertThat(resposta.getContentAsString()).doesNotContain("detalhe interno");
	}

	@Test
	void ocampoDosErrosSaiNaOrdemDoErrorResponse() throws IOException {
		MockHttpServletResponse resposta = new MockHttpServletResponse();

		new RestAuthenticationEntryPoint(writer).commence(new MockHttpServletRequest(), resposta,
				new BadCredentialsException("x"));

		assertThat(resposta.getContentAsString()).startsWith("{\"dataHora\":\"")
				.contains("\",\"status\":401,\"erro\":\"Não autenticado\",\"mensagem\":");
	}

}
