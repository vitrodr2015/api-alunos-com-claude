package br.com.vitor.apicrud_alunos.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.vitor.apicrud_alunos.security.InvalidCredentialsException;
import br.com.vitor.apicrud_alunos.security.InvalidTokenException;

@WebMvcTest(GlobalExceptionHandlerTests.FakeController.class)
@Import({ GlobalExceptionHandler.class, GlobalExceptionHandlerTests.FakeController.class })
class GlobalExceptionHandlerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void recursoInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/fake/nao-encontrado"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.erro").value("Não encontrado"))
				.andExpect(jsonPath("$.mensagem").value("Aluno não encontrado"))
				.andExpect(jsonPath("$.dataHora").exists());
	}

	@Test
	void requisicaoInvalidaRetorna400() throws Exception {
		mockMvc.perform(get("/fake/invalido"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("Dados inválidos"));
	}

	@Test
	void tokenInvalidoRetorna401ComOMesmoFormatoDeErro() throws Exception {
		mockMvc.perform(get("/fake/token-invalido"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.erro").value("Não autenticado"))
				.andExpect(jsonPath("$.mensagem").value("Token expirado"))
				.andExpect(jsonPath("$.dataHora").exists());
	}

	@Test
	void credenciaisInvalidasRetorna401ComAMensagemGenerica() throws Exception {
		mockMvc.perform(get("/fake/credenciais"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.erro").value("Não autenticado"))
				.andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));
	}

	@Test
	void violacaoDeRestricaoDoBancoRetorna409SemVazarODetalhe() throws Exception {
		mockMvc.perform(get("/fake/integridade"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.erro").value("Conflito"))
				.andExpect(jsonPath("$.mensagem").value("O registro entra em conflito com um já existente"))
				.andExpect(content().string(not(containsString("uk_aluno_email"))));
	}

	@Test
	void conflitoRetorna409() throws Exception {
		mockMvc.perform(get("/fake/conflito"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.mensagem").value("Aluno já cadastrado"));
	}

	@Test
	void idComTipoErradoRetorna400() throws Exception {
		mockMvc.perform(get("/fake/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("O valor 'abc' é inválido para 'id'"));
	}

	@Test
	void parametroAusenteRetorna400() throws Exception {
		mockMvc.perform(get("/fake/parametro"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("O parâmetro 'nome' é obrigatório"));
	}

	@Test
	void corpoIlegivelRetorna400() throws Exception {
		mockMvc.perform(post("/fake/corpo").contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void metodoNaoPermitidoRetorna405() throws Exception {
		mockMvc.perform(post("/fake/conflito"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void formatoDeCorpoNaoAceitoRetorna415() throws Exception {
		mockMvc.perform(post("/fake/corpo").contentType(MediaType.TEXT_PLAIN).content("texto"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.status").value(415));
	}

	@Test
	void acceptNaoSuportadoRetorna406() throws Exception {
		mockMvc.perform(get("/outro/somente-json").accept(MediaType.APPLICATION_XML))
				.andExpect(status().isNotAcceptable())
				.andExpect(jsonPath("$.status").value(406));
	}

	@Test
	void rotaInexistenteRetorna404() throws Exception {
		mockMvc.perform(get("/fake/rota/que/nao/existe"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void erroInesperadoRetorna500SemDetalhes() throws Exception {
		mockMvc.perform(get("/fake/erro"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.mensagem").value("Erro interno do servidor"));
	}

	// ---------- infraestrutura: banco fora do ar = 503; URL mal codificada = 400 (QA) ----------

	@Test
	void bancoForaDoArRetorna503ComRetryAfterESemVazarDetalhes() throws Exception {
		mockMvc.perform(get("/fake/banco-fora"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "5"))
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.erro").value("Serviço indisponível"))
				.andExpect(jsonPath("$.mensagem").value(containsString("indisponível")))
				.andExpect(content().string(not(containsString("jdbc"))))
				.andExpect(content().string(not(containsString("segredo"))));
	}

	@Test
	void bancoForaDoArEmbrulhadoEmOutraExcecaoTambemRetorna503() throws Exception {
		mockMvc.perform(get("/fake/banco-fora-embrulhado")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.erro").value("Serviço indisponível"))
				.andExpect(content().string(not(containsString("host-do-banco"))));
	}

	@Test
	void erroDeTransacaoQueNaoEDoBancoForaDoArContinuaSendo500() throws Exception {
		mockMvc.perform(get("/fake/erro-de-commit")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.mensagem").value("Erro interno do servidor"));
		mockMvc.perform(get("/fake/erro-comum")).andExpect(status().isInternalServerError());
	}

	@Test
	void parametroDaUrlMalCodificadoRetorna400() throws Exception {
		mockMvc.perform(get("/fake/parametro-ruim")).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.mensagem").value("Parâmetro da URL com codificação inválida"))
				.andExpect(content().string(not(containsString("%ZZ"))));
	}

	@RestController
	static class FakeController {

		@GetMapping("/fake/banco-fora")
		String bancoFora() {
			throw new org.springframework.transaction.CannotCreateTransactionException("jdbc:mysql://segredo:3306/x indisponivel");
		}

		@GetMapping("/fake/banco-fora-embrulhado")
		String bancoForaEmbrulhado() {
			throw new RuntimeException("falha qualquer", new org.springframework.dao.DataAccessResourceFailureException(
					"Unable to acquire JDBC Connection", new java.sql.SQLTransientConnectionException("host-do-banco: timeout")));
		}

		@GetMapping("/fake/erro-de-commit")
		String erroDeCommit() {
			throw new org.springframework.transaction.TransactionSystemException("Could not commit JPA transaction");
		}

		@GetMapping("/fake/erro-comum")
		String erroComum() {
			throw new IllegalStateException("bug do programador");
		}

		@GetMapping("/fake/parametro-ruim")
		String parametroRuim() {
			throw new org.apache.tomcat.util.http.InvalidParameterException("Character decoding failed. Parameter [nome] with value [%ZZ]");
		}


		@GetMapping("/fake/nao-encontrado")
		String naoEncontrado() {
			throw new ResourceNotFoundException("Aluno não encontrado");
		}

		@GetMapping("/fake/invalido")
		String invalido() {
			throw new InvalidRequestException("Dados inválidos");
		}

		@GetMapping(value = "/fake/conflito", produces = MediaType.APPLICATION_JSON_VALUE)
		String conflito() {
			throw new ConflictException("Aluno já cadastrado");
		}

		@GetMapping("/fake/{id}")
		String porId(@PathVariable Long id) {
			return "ok";
		}

		@GetMapping("/fake/parametro")
		String parametro(@RequestParam String nome) {
			return nome;
		}

		@PostMapping(value = "/fake/corpo", consumes = MediaType.APPLICATION_JSON_VALUE)
		String corpo(@RequestBody Map<String, String> corpo) {
			return "ok";
		}

		@GetMapping(value = "/outro/somente-json", produces = MediaType.APPLICATION_JSON_VALUE)
		String somenteJson() {
			return "ok";
		}

		@GetMapping("/fake/credenciais")
		String credenciais() {
			throw new InvalidCredentialsException();
		}

		@GetMapping("/fake/integridade")
		String integridade() {
			throw new DataIntegrityViolationException("Duplicate entry 'x' for key 'uk_aluno_email'");
		}

		@GetMapping("/fake/token-invalido")
		String tokenInvalido() {
			throw new InvalidTokenException("Token expirado");
		}

		@GetMapping("/fake/erro")
		String erro() {
			throw new IllegalStateException("detalhe interno que não pode vazar");
		}

	}

}
