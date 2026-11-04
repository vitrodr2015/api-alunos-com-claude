package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.vitor.apicrud_alunos.dto.RegisterRequest;
import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.repository.UserRepository;
import br.com.vitor.apicrud_alunos.security.InvalidCredentialsException;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import br.com.vitor.apicrud_alunos.security.TokenPair;

// Testes unitários: tudo simulado (Mockito), sem Spring e sem banco
@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtTokenProvider tokenProvider;

	@Mock
	private UserRepository userRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthService service;

	private static TokenPair par() {
		return new TokenPair("acesso", Instant.now(), "renovacao", Instant.now());
	}

	private static Permission permissao(String descricao) {
		Permission p = new Permission();
		p.setDescription(descricao);
		return p;
	}

	// ---------- signIn ----------

	@Test
	void signInComCredenciaisCertasGeraOParDeTokensDoUsuarioAutenticado() {
		User usuario = new User();
		usuario.setUserName("maria");
		var autenticado = UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
		when(authenticationManager.authenticate(any())).thenReturn(autenticado);
		when(tokenProvider.createTokenPair(usuario)).thenReturn(par());

		TokenPair resultado = service.signIn("maria", "senha");

		assertThat(resultado.accessToken()).isEqualTo("acesso");
		ArgumentCaptor<UsernamePasswordAuthenticationToken> pedido = ArgumentCaptor
				.forClass(UsernamePasswordAuthenticationToken.class);
		verify(authenticationManager).authenticate(pedido.capture());
		assertThat(pedido.getValue().getName()).isEqualTo("maria");
		assertThat(pedido.getValue().getCredentials()).isEqualTo("senha");
	}

	@Test
	void qualquerFalhaDeAutenticacaoViraAMesmaInvalidCredentialsException() {
		List<RuntimeException> falhas = List.of(
				new BadCredentialsException("senha errada"),
				new UsernameNotFoundException("não existe"),
				new DisabledException("desativado"),
				new LockedException("bloqueado"),
				new AccountExpiredException("expirada"),
				new CredentialsExpiredException("credenciais expiradas"),
				new InternalAuthenticationServiceException("erro interno do provedor"));

		for (RuntimeException falha : falhas) {
			// doThrow().when() e não when().thenThrow(): ao reprogramar o mock no laço, o when() chamaria o método
			// que já está programado para lançar a exceção da volta anterior
			doThrow(falha).when(authenticationManager).authenticate(any());
			assertThatThrownBy(() -> service.signIn("alguem", "senha"))
					.isInstanceOf(InvalidCredentialsException.class)
					.hasMessage("Credenciais inválidas");
		}
		verifyNoInteractions(tokenProvider);
	}

	// ---------- refresh ----------

	@Test
	void refreshDelegaAoProvedorDeTokens() {
		when(tokenProvider.renew("o-refresh")).thenReturn(par());

		assertThat(service.refresh("o-refresh").refreshToken()).isEqualTo("renovacao");
	}

	// ---------- register ----------

	private static RegisterRequest pedido(String senha) {
		return new RegisterRequest("novo.usuario", "Novo Usuário", senha);
	}

	@Test
	void registerAplicaOHashDaSenhaEAtribuiAPermissaoPadraoUser() {
		Permission user = permissao("USER");
		when(userRepository.existsByUserNameIgnoreCase("novo.usuario")).thenReturn(false);
		when(permissionRepository.findByDescription("USER")).thenReturn(Optional.of(user));
		when(passwordEncoder.encode("SenhaForte#123")).thenReturn("{bcrypt}hash-gerado");
		when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

		User criado = service.register(pedido("SenhaForte#123"));

		assertThat(criado.getPassword()).isEqualTo("{bcrypt}hash-gerado").isNotEqualTo("SenhaForte#123");
		assertThat(criado.getUsername()).isEqualTo("novo.usuario");
		assertThat(criado.getFullName()).isEqualTo("Novo Usuário");
		assertThat(criado.getPermissions()).containsExactly(user);
		assertThat(criado.isEnabled()).isTrue();
	}

	@Test
	void registerComNomeJaExistenteLancaConflitoSemTocarNaSenha() {
		when(userRepository.existsByUserNameIgnoreCase("novo.usuario")).thenReturn(true);

		assertThatThrownBy(() -> service.register(pedido("SenhaForte#123")))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Já existe um usuário com este nome");
		verifyNoInteractions(passwordEncoder);
		verify(userRepository, never()).save(any());
	}

	@Test
	void registerComSenhaDeMaisDe72BytesLancaRequisicaoInvalidaAntesDeQualquerConsulta() {
		// 40 letras acentuadas: 40 caracteres, mas 80 bytes
		assertThatThrownBy(() -> service.register(pedido("á".repeat(40))))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("72 bytes");
		verifyNoInteractions(userRepository, permissionRepository, passwordEncoder);
	}

	@Test
	void registerComSenhaDeExatamente72BytesPassaDaValidacaoDeTamanho() {
		when(userRepository.existsByUserNameIgnoreCase("novo.usuario")).thenReturn(false);
		when(permissionRepository.findByDescription("USER")).thenReturn(Optional.of(permissao("USER")));
		when(passwordEncoder.encode(any())).thenReturn("{bcrypt}h");
		when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

		assertThat(service.register(pedido("a".repeat(72)))).isNotNull();
	}

	@Test
	void registerSemAPermissaoPadraoNoBancoFalhaComErroDeConfiguracaoEnaoCriaUsuarioSemPermissao() {
		when(userRepository.existsByUserNameIgnoreCase("novo.usuario")).thenReturn(false);
		when(permissionRepository.findByDescription("USER")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.register(pedido("SenhaForte#123")))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("USER");
		verify(userRepository, never()).save(any());
	}

}
