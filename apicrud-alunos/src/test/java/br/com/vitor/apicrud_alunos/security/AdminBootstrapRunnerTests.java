package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.repository.UserRepository;

// Testes unitários da criação controlada do primeiro administrador: sem Spring e sem banco
@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTests {

	private static final String SENHA_BOA = "Senha-De-Teste-12345";

	@Mock
	private UserRepository userRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private AdminBootstrapRunner runner(String usuario, String senha) {
		return new AdminBootstrapRunner(userRepository, permissionRepository, passwordEncoder, usuario, senha);
	}

	private static Permission permissao(String descricao) {
		Permission p = new Permission();
		p.setDescription(descricao);
		return p;
	}

	// ---------- desligado por padrão ----------

	@Test
	void semASenhaConfiguradaNaoFazNadaEnemTocaNoBanco() {
		for (String vazia : new String[] { null, "" }) {
			runner("admin", vazia).run(null);
		}
		verifyNoInteractions(userRepository, permissionRepository, passwordEncoder);
	}

	// ---------- criação ----------

	@Test
	void criaOAdminComHashDaSenhaEAsPermissoesAdminEUser() {
		Permission admin = permissao("ADMIN");
		Permission user = permissao("USER");
		when(userRepository.existsByPermissionsDescription("ADMIN")).thenReturn(false);
		when(userRepository.existsByUserNameIgnoreCase("chefe")).thenReturn(false);
		when(permissionRepository.findByDescription("ADMIN")).thenReturn(Optional.of(admin));
		when(permissionRepository.findByDescription("USER")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode(SENHA_BOA)).thenReturn("{bcrypt}hash-gerado");

		runner("chefe", SENHA_BOA).run(null);

		ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(salvo.capture());
		assertThat(salvo.getValue().getUsername()).isEqualTo("chefe");
		assertThat(salvo.getValue().getPassword()).isEqualTo("{bcrypt}hash-gerado").isNotEqualTo(SENHA_BOA);
		assertThat(salvo.getValue().getPermissions()).containsExactly(admin, user);
		assertThat(salvo.getValue().isEnabled()).isTrue();
	}

	@Test
	void seJaExisteUmAdministradorNaoCriaNemTrocaNada() {
		when(userRepository.existsByPermissionsDescription("ADMIN")).thenReturn(true);

		runner("admin", SENHA_BOA).run(null);

		verify(userRepository, never()).save(any());
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void seExisteUmUsuarioComumComOMesmoNomeAPartidaFalhaEnaoOPromove() {
		when(userRepository.existsByPermissionsDescription("ADMIN")).thenReturn(false);
		when(userRepository.existsByUserNameIgnoreCase("admin")).thenReturn(true);

		assertThatThrownBy(() -> runner("admin", SENHA_BOA).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("não é administrador");
		verify(userRepository, never()).save(any());
	}

	@Test
	void seAPermissaoAdminNaoExisteNoBancoFalha() {
		when(userRepository.existsByPermissionsDescription("ADMIN")).thenReturn(false);
		when(userRepository.existsByUserNameIgnoreCase("admin")).thenReturn(false);
		when(permissionRepository.findByDescription("ADMIN")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> runner("admin", SENHA_BOA).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("ADMIN");
		verify(userRepository, never()).save(any());
	}

	// ---------- validação da senha e do nome: a subida falha, nada é criado ----------

	@Test
	void senhaCurtaDemaisFazAPartidaFalhar() {
		assertThatThrownBy(() -> runner("admin", "a".repeat(AdminBootstrapRunner.TAMANHO_MINIMO_DA_SENHA - 1)).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("curta demais");
		verifyNoInteractions(passwordEncoder);
		verify(userRepository, never()).save(any());
	}

	@Test
	void senhaDeExatamente12CaracteresEAceitaNaValidacao() {
		// só a validação: com o ADMIN já existindo a execução termina sem criar nada, mas passou da validação
		when(userRepository.existsByPermissionsDescription("ADMIN")).thenReturn(true);

		assertThatCode(() -> runner("admin", "a".repeat(AdminBootstrapRunner.TAMANHO_MINIMO_DA_SENHA)).run(null))
				.doesNotThrowAnyException();
	}

	@Test
	void senhaComMaisDe72BytesFazAPartidaFalhar() {
		// 40 letras acentuadas: 40 caracteres, mas 80 bytes
		assertThatThrownBy(() -> runner("admin", "á".repeat(40)).run(null))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("72 bytes");
	}

	@Test
	void senhaComEspacoOuQuebraDeLinhaNasPontasFazAPartidaFalhar() {
		// o erro clássico de gravar a variável com "echo", que acrescenta uma quebra de linha no fim
		for (String suja : new String[] { SENHA_BOA + "\n", SENHA_BOA + " ", " " + SENHA_BOA, SENHA_BOA + "\r\n" }) {
			assertThatThrownBy(() -> runner("admin", suja).run(null))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("espaço ou quebra de linha");
		}
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void nomeDeUsuarioInvalidoFazAPartidaFalhar() {
		for (String ruim : new String[] { "", "ab", "com espaco", "nome;drop", "a".repeat(101), null }) {
			assertThatThrownBy(() -> runner(ruim, SENHA_BOA).run(null))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("ADMIN_BOOTSTRAP_USERNAME");
		}
	}

}
