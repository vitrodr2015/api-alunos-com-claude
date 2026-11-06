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

	// QA: o MySQL compara sem diferenciar acentos, então "joão" achava o usuário "joao". O nome é conferido de novo.
	@Test
	void loadUserByUsernameNaoAceitaNomeQueSoDifereNoAcento() {
		User existente = new User();
		existente.setUserName("joao");
		when(repository.findByUserName("joão")).thenReturn(Optional.of(existente)); // o que o banco devolveria

		assertThatThrownBy(() -> service.loadUserByUsername("joão")).isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void loadUserByUsernameContinuaIgnorandoMaiusculas() {
		User existente = new User();
		existente.setUserName("maria");
		when(repository.findByUserName("MARIA")).thenReturn(Optional.of(existente));

		assertThat(service.loadUserByUsername("MARIA")).isSameAs(existente);
	}

	@Test
	void loadUserByUsernameNaoAceitaEspacoNoFimQueOBancoIgnora() {
		User existente = new User();
		existente.setUserName("maria");
		when(repository.findByUserName("maria ")).thenReturn(Optional.of(existente));

		assertThatThrownBy(() -> service.loadUserByUsername("maria ")).isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void loadUserByUsernameLancaUsernameNotFoundQuandoNaoExiste() {
		when(repository.findByUserName("ninguem")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.loadUserByUsername("ninguem"))
				.isInstanceOf(UsernameNotFoundException.class)
				.hasMessage("Usuário não encontrado");
	}

}
