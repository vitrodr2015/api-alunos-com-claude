package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.model.User;
import jakarta.persistence.EntityManager;

// A senha nunca é gravada em texto: o banco recebe o hash, e a autenticação confere a senha digitada contra ele.
// Cada teste roda numa transação desfeita no fim (nada fica no banco).
@SpringBootTest
@Transactional
class PasswordHashingIntegrationTests {

	private static final String SENHA = "senha-de-estudo-" + UUID.randomUUID();

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private EntityManager em;

	private String gravarUsuarioComSenha(String senha) {
		String nome = "usuario-" + UUID.randomUUID().toString().substring(0, 8);
		User u = new User();
		u.setUserName(nome);
		u.setFullName("Nome Completo de " + nome);
		u.setPassword(passwordEncoder.encode(senha));
		em.persist(u);
		em.flush();
		em.clear();
		return nome;
	}

	@Test
	void oBeanDoCodificadorUsaOPrefixoBcrypt() {
		assertThat(passwordEncoder.encode("qualquer")).startsWith("{bcrypt}");
	}

	@Test
	void obancoGuardaOHashEnuncaASenhaEmTexto() {
		String nome = gravarUsuarioComSenha(SENHA);

		String gravado = (String) em.createNativeQuery("select password from users where user_name = ?1")
				.setParameter(1, nome).getSingleResult();

		assertThat(gravado).startsWith("{bcrypt}$2");
		assertThat(gravado).doesNotContain(SENHA);
	}

	@Test
	void autenticaComASenhaCertaContraOHashDoBanco() {
		String nome = gravarUsuarioComSenha(SENHA);
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);

		Authentication resultado = provider.authenticate(new UsernamePasswordAuthenticationToken(nome, SENHA));

		assertThat(resultado.isAuthenticated()).isTrue();
		assertThat(resultado.getName()).isEqualTo(nome);
	}

	@Test
	void recusaASenhaErrada() {
		String nome = gravarUsuarioComSenha(SENHA);
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);

		assertThatThrownBy(() -> provider.authenticate(new UsernamePasswordAuthenticationToken(nome, "senha-errada")))
				.isInstanceOf(BadCredentialsException.class);
	}

}
