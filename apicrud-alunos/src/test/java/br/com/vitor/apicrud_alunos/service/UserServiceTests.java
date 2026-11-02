package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.UserRepository;

// Testes unitários: o repository é simulado (Mockito), sem Spring e sem banco
@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	@Mock
	private UserRepository repository;

	@InjectMocks
	private UserService service;

	@Test
	void loadUserByUsernameDevolveOUsuarioEncontrado() {
		User existente = new User();
		existente.setUserName("maria");
		when(repository.findByUserName("maria")).thenReturn(Optional.of(existente));

		UserDetails carregado = service.loadUserByUsername("maria");

		assertThat(carregado).isSameAs(existente);
		assertThat(carregado.getUsername()).isEqualTo("maria");
		verify(repository).findByUserName("maria");
	}

	@Test
	void loadUserByUsernameLancaUsernameNotFoundQuandoNaoExiste() {
		when(repository.findByUserName("ninguem")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("ninguem"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("Usuário não encontrado");
	}

}
