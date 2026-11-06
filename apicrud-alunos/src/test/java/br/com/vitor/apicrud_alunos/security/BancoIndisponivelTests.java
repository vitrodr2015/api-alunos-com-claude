package br.com.vitor.apicrud_alunos.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.AlunoRepository;
import br.com.vitor.apicrud_alunos.repository.UserRepository;

// QA: com o BANCO FORA DO AR a API respondia 401 (parecia token vencido) ou 500, depois de 30 s. Agora responde 503 com
// Retry-After, em JSON e sem vazar nada, em TODOS os caminhos: filtro do JWT, login, refresh e rotas de dados.
// Filtros ligados; os repositórios são simulados (lançam as exceções que o Spring lança quando o banco cai).
@SpringBootTest
@AutoConfigureMockMvc
class BancoIndisponivelTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenProvider provider;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AlunoRepository alunoRepository;

	private static User usuario(String nome, String... permissoes) {
		User u = new User();
		u.setUserName(nome);
		u.setFullName("Fulano de Tal");
		u.setPassword("{bcrypt}hash-ficticio");
		List<Permission> lista = new ArrayList<>();
		for (String descricao : permissoes) {
			Permission p = new Permission();
			p.setDescription(descricao);
			lista.add(p);
		}
		u.setPermissions(lista);
		return u;
	}

	private static Exception bancoFora() {
		return new DataAccessResourceFailureException("Unable to acquire JDBC Connection: jdbc:mysql://segredo:3306/x",
				new SQLTransientConnectionException("HikariPool-1 - Connection is not available, request timed out after 5000ms"));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private void esperar503(org.springframework.test.web.servlet.ResultActions r) throws Exception {
		r.andExpect(status().isServiceUnavailable())
				.andExpect(header().string(HttpHeaders.RETRY_AFTER, "5"))
				.andExpect(jsonPath("$.status").value(503))
				.andExpect(jsonPath("$.erro").value("Serviço indisponível"))
				.andExpect(jsonPath("$.mensagem").value(containsString("Tente novamente")))
				.andExpect(content().string(not(containsString("jdbc"))))
				.andExpect(content().string(not(containsString("segredo"))))
				.andExpect(content().string(not(containsString("Hikari"))));
	}

	// ---------- o token é recarregado do banco: banco fora = 503, não "token inválido" ----------

	@Test
	void rotaProtegidaComOBancoForaDa503EnaoUmEngano401() throws Exception {
		String token = provider.createTokenPair(usuario("fulano", "USER")).accessToken();
		when(userRepository.findByUserName("fulano"))
				.thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

		esperar503(mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(token))));
	}

	@Test
	void loginComOBancoForaDa503() throws Exception {
		when(userRepository.findByUserName(anyString()))
				.thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

		esperar503(mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"fulano\",\"password\":\"qualquer-senha\"}")));
	}

	@Test
	void refreshComOBancoForaDa503() throws Exception {
		String refresh = provider.createTokenPair(usuario("fulano", "USER")).refreshToken();
		when(userRepository.findByUserName("fulano"))
				.thenThrow(new CannotCreateTransactionException("Could not open JPA EntityManager for transaction"));

		esperar503(mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}")));
	}

	// ---------- o usuário carrega, mas o banco cai nas rotas de dados ----------

	@Test
	void listarComOBancoForaDa503() throws Exception {
		User fulano = usuario("fulano", "USER");
		when(userRepository.findByUserName("fulano")).thenReturn(Optional.of(fulano));
		when(alunoRepository.findByNomeContainingIgnoreCase(anyString(), any(Pageable.class))).thenThrow(bancoFora());

		esperar503(mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(provider.createTokenPair(fulano).accessToken()))));
	}

	@Test
	void criarEApagarComOBancoForaDao503() throws Exception {
		User admin = usuario("chefe", "ADMIN", "USER");
		when(userRepository.findByUserName("chefe")).thenReturn(Optional.of(admin));
		when(alunoRepository.existsByEmailIgnoreCase(anyString())).thenThrow(bancoFora());
		when(alunoRepository.apagarPorId(anyLong())).thenThrow(bancoFora());
		String token = bearer(provider.createTokenPair(admin).accessToken());

		esperar503(mockMvc.perform(post("/alunos").header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Aluno Teste\",\"email\":\"a@b.com\",\"dataNascimento\":\"2000-01-01\"}")));
		esperar503(mockMvc.perform(delete("/alunos/1").header(HttpHeaders.AUTHORIZATION, token)));
	}

	// ---------- o que NÃO muda ----------

	@Test
	void tokenInvalidoContinuaSendo401MesmoComOBancoFora() throws Exception {
		when(userRepository.findByUserName(anyString())).thenThrow(new CannotCreateTransactionException("banco fora"));

		mockMvc.perform(get("/alunos")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer("isto-nao-e-um-token"))).andExpect(status().isUnauthorized());
	}

	@Test
	void erroQueNaoEDoBancoForaDoArContinuaSendo500() throws Exception {
		User fulano = usuario("fulano", "USER");
		when(userRepository.findByUserName("fulano")).thenReturn(Optional.of(fulano));
		when(alunoRepository.findByNomeContainingIgnoreCase(anyString(), any(Pageable.class)))
				.thenThrow(new TransactionSystemException("Could not commit JPA transaction"));
		String token = bearer(provider.createTokenPair(fulano).accessToken());

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.mensagem").value("Erro interno do servidor"));
	}

	@Test
	void usuarioQueNaoExisteMaisContinuaSendo401() throws Exception {
		String token = provider.createTokenPair(usuario("sumiu", "USER")).accessToken();
		when(userRepository.findByUserName("sumiu")).thenReturn(Optional.empty());

		mockMvc.perform(get("/alunos").header(HttpHeaders.AUTHORIZATION, bearer(token))).andExpect(status().isUnauthorized());
	}

}
