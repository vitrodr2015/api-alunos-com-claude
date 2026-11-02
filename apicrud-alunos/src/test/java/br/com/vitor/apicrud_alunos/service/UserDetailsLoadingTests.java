package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import jakarta.persistence.EntityManager;

// "Pronto quando": o Spring Security consegue carregar o usuário e as permissões pelo nome.
// Cada teste roda numa transação que é desfeita no fim (nada fica no banco). O flush() grava e o clear() esvazia
// o cache do Hibernate, para a leitura passar de verdade pelo SQL das tabelas users, permission e user_permission.
@SpringBootTest
@Transactional
class UserDetailsLoadingTests {

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private EntityManager em;

	private Permission permissao(String descricao) {
		Permission p = new Permission();
		p.setDescription(descricao + "_" + UUID.randomUUID().toString().substring(0, 8));
		em.persist(p);
		return p;
	}

	private User usuario(String nome, Permission... permissoes) {
		User u = new User();
		u.setUserName(nome);
		u.setFullName("Nome Completo de " + nome);
		u.setPassword("{noop}hash-de-teste"); // texto fictício de teste; o hash real vem no passo 30
		u.setPermissions(List.of(permissoes));
		em.persist(u);
		return u;
	}

	private static String nomeUnico() {
		return "usuario-" + UUID.randomUUID().toString().substring(0, 8);
	}

	@Test
	void carregaOUsuarioEAsPermissoesPeloNome() {
		Permission admin = permissao("ADMIN");
		Permission leitura = permissao("LEITURA");
		String nome = nomeUnico();
		usuario(nome, admin, leitura);
		em.flush();
		em.clear();

		UserDetails carregado = userDetailsService.loadUserByUsername(nome);

		assertThat(carregado.getUsername()).isEqualTo(nome);
		assertThat(carregado.getPassword()).isEqualTo("{noop}hash-de-teste");
		assertThat(carregado.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder(admin.getDescription(), leitura.getDescription());
		assertThat(carregado.isEnabled()).isTrue();
		assertThat(carregado.isAccountNonLocked()).isTrue();
		assertThat(carregado.isAccountNonExpired()).isTrue();
		assertThat(carregado.isCredentialsNonExpired()).isTrue();
	}

	@Test
	void usuarioSemPermissoesTemListaVaziaDeAutoridades() {
		String nome = nomeUnico();
		usuario(nome);
		em.flush();
		em.clear();

		assertThat(userDetailsService.loadUserByUsername(nome).getAuthorities()).isEmpty();
	}

	@Test
	void flagsDeContaDesativadaOuBloqueadaSaoPreservadas() {
		String nome = nomeUnico();
		User u = usuario(nome);
		u.setEnabled(false);
		u.setAccountNonLocked(false);
		u.setAccountNonExpired(false);
		u.setCredentialsNonExpired(false);
		em.flush();
		em.clear();

		UserDetails carregado = userDetailsService.loadUserByUsername(nome);

		assertThat(carregado.isEnabled()).isFalse();
		assertThat(carregado.isAccountNonLocked()).isFalse();
		assertThat(carregado.isAccountNonExpired()).isFalse();
		assertThat(carregado.isCredentialsNonExpired()).isFalse();
	}

	@Test
	void nomeInexistenteLancaUsernameNotFoundException() {
		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nao-existe-" + UUID.randomUUID()))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void oBeanDeUserDetailsServiceEOUserService() {
		assertThat(userDetailsService).isInstanceOf(UserService.class);
	}

}
