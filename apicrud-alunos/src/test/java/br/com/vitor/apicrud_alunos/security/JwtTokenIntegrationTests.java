package br.com.vitor.apicrud_alunos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import jakarta.persistence.EntityManager;

// O provedor de tokens com o UserService de verdade: o usuário e as permissões são recarregados do BANCO a cada
// validação. Cada teste roda numa transação desfeita no fim (nada fica no banco); flush() + clear() fazem a leitura
// passar de verdade pelo SQL, sem o cache do Hibernate.
@SpringBootTest
@Transactional
class JwtTokenIntegrationTests {

	@Autowired
	private JwtTokenProvider provider;

	@Autowired
	private EntityManager em;

	private User usuario(Permission... permissoes) {
		User u = new User();
		u.setUserName("usuario-" + UUID.randomUUID().toString().substring(0, 8));
		u.setFullName("Nome Completo");
		u.setPassword("{bcrypt}hash-ficticio");
		u.setPermissions(List.of(permissoes));
		em.persist(u);
		return u;
	}

	private Permission permissao(String descricao) {
		Permission p = new Permission();
		p.setDescription(descricao + "_" + UUID.randomUUID().toString().substring(0, 8));
		em.persist(p);
		return p;
	}

	private void gravarELimpar() {
		em.flush();
		em.clear();
	}

	@Test
	void obeanDoProvedorSobeComOSegredoDoApplicationYml() {
		assertThat(provider).isNotNull();
	}

	@Test
	void tokenEmitidoParaUsuarioDoBancoAutenticaECarregaAsPermissoes() {
		Permission admin = permissao("ADMIN");
		User u = usuario(admin);
		gravarELimpar();

		TokenPair par = provider.createTokenPair(u);
		Authentication autenticacao = provider.getAuthentication(par.accessToken());

		assertThat(autenticacao.getName()).isEqualTo(u.getUsername());
		assertThat(autenticacao.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly(admin.getDescription());
	}

	@Test
	void usuarioDesativadoNoBancoTemOTokenRecusadoNaProximaValidacao() {
		User u = usuario();
		gravarELimpar();
		String token = provider.createTokenPair(u).accessToken();
		assertThat(provider.getAuthentication(token).isAuthenticated()).isTrue();

		User noBanco = em.find(User.class, u.getId());
		noBanco.setEnabled(false);
		gravarELimpar();

		assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void usuarioBloqueadoNoBancoTemOTokenRecusado() {
		User u = usuario();
		gravarELimpar();
		String token = provider.createTokenPair(u).accessToken();

		em.find(User.class, u.getId()).setAccountNonLocked(false);
		gravarELimpar();

		assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void usuarioApagadoDoBancoTemOTokenRecusado() {
		User u = usuario();
		gravarELimpar();
		String token = provider.createTokenPair(u).accessToken();

		em.remove(em.find(User.class, u.getId()));
		gravarELimpar();

		assertThatThrownBy(() -> provider.getAuthentication(token)).isInstanceOf(InvalidTokenException.class);
	}

	@Test
	void permissaoRevogadaNoBancoSaiDoTokenRenovado() {
		Permission admin = permissao("ADMIN");
		User u = usuario(admin);
		gravarELimpar();
		String refresh = provider.createTokenPair(u).refreshToken();

		em.find(User.class, u.getId()).setPermissions(List.of());
		gravarELimpar();
		TokenPair novo = provider.renew(refresh);

		assertThat(com.auth0.jwt.JWT.decode(novo.accessToken()).getClaim("roles").asList(String.class)).isEmpty();
	}

}
