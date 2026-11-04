package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.UserRepository;

// A criação controlada do primeiro administrador com a aplicação inteira: a variável definida na subida cria o ADMIN
// de verdade (num contexto novo, com H2 próprio, que só tem as permissões ADMIN e USER do data.sql).
@SpringBootTest(properties = { "app.admin-bootstrap.username=chefe.teste", "app.admin-bootstrap.password=Senha-Bootstrap-De-Teste-123" })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AdminBootstrapIntegrationTests {

	private static final String SENHA = "Senha-Bootstrap-De-Teste-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AdminBootstrapRunner runner;

	@Test
	void aVariavelDefinidaNaSubidaCriaOAdministradorComAsDuasPermissoes(CapturedOutput saida) {
		User admin = userRepository.findByUserName("chefe.teste").orElseThrow();

		assertThat(admin.getPermissions()).extracting(p -> p.getDescription()).containsExactlyInAnyOrder("ADMIN", "USER");
		assertThat(admin.isEnabled()).isTrue();
		assertThat(saida.getAll()).contains("Administrador inicial 'chefe.teste' criado").contains("REMOVA essa variável");
	}

	@Test
	void aSenhaFicaGravadaSoComoHashEFuncionaNoLogin() throws Exception {
		User admin = userRepository.findByUserName("chefe.teste").orElseThrow();
		assertThat(admin.getPassword()).startsWith("{bcrypt}$2").doesNotContain(SENHA);

		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"chefe.teste\",\"password\":\"" + SENHA + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString());
		mockMvc.perform(post("/auth/signin").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"chefe.teste\",\"password\":\"" + SENHA + "x\"}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void aSenhaNuncaApareceNoLog(CapturedOutput saida) {
		assertThat(saida.getAll()).doesNotContain(SENHA);
	}

	@Test
	void rodarDeNovoComAVariavelAindaDefinidaNaoRecriaNemTrocaASenha(CapturedOutput saida) {
		String hashAntes = userRepository.findByUserName("chefe.teste").orElseThrow().getPassword();
		long totalAntes = userRepository.count();

		runner.run(null);

		assertThat(userRepository.count()).isEqualTo(totalAntes);
		assertThat(userRepository.findByUserName("chefe.teste").orElseThrow().getPassword()).isEqualTo(hashAntes);
		assertThat(saida.getAll()).contains("já existe um administrador").doesNotContain(SENHA);
	}

}
