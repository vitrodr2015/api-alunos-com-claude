package br.com.vitor.apicrud_alunos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// Testes unitários do codificador de senha: sem Spring e sem banco
class PasswordConfigTests {

	private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

	@Test
	void ohashNaoEIgualASenhaEComecaComOPrefixoDoAlgoritmo() {
		String hash = encoder.encode("senha-de-estudo");

		assertThat(hash).isNotEqualTo("senha-de-estudo");
		assertThat(hash).doesNotContain("senha-de-estudo");
		assertThat(hash).startsWith("{bcrypt}$2");
	}

	@Test
	void aMesmaSenhaGeraHashesDiferentesPorCausaDoSalt() {
		assertThat(encoder.encode("senha-de-estudo")).isNotEqualTo(encoder.encode("senha-de-estudo"));
	}

	@Test
	void confereASenhaCertaERecusaAErrada() {
		String hash = encoder.encode("senha-de-estudo");

		assertThat(encoder.matches("senha-de-estudo", hash)).isTrue();
		assertThat(encoder.matches("senha-errada", hash)).isFalse();
		assertThat(encoder.matches("Senha-de-estudo", hash)).isFalse();
	}

	@Test
	void hashSemPrefixoOuComPrefixoDesconhecidoEhRecusadoComErro() {
		// senha em texto no banco, ou algoritmo que não cadastramos, nunca é aceito
		assertThatThrownBy(() -> encoder.matches("qualquer", "texto-puro-sem-prefixo"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> encoder.matches("qualquer", "{noop}qualquer"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> encoder.matches("qualquer", "{md5}34e2e4fb9f2d0f8d8f3d4d6a5a1e8e1c"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void senhaDeAte72BytesFunciona() {
		String senha72 = "a".repeat(72);

		assertThat(encoder.matches(senha72, encoder.encode(senha72))).isTrue();
		// o último caractere conta: nada é truncado em silêncio
		assertThat(encoder.matches("a".repeat(71) + "b", encoder.encode(senha72))).isFalse();
	}

	@Test
	void senhaComMaisDe72BytesEhRecusadaAoCodificar() {
		// limite do BCrypt. Um futuro cadastro de usuário deve validar o tamanho e responder 400, não deixar virar 500.
		assertThatThrownBy(() -> encoder.encode("a".repeat(73)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("72 bytes");
	}

	@Test
	void hashAntigoComPrefixoDeAlgoritmoCadastradoContinuaValendo() {
		// simula uma senha gravada antes de uma eventual troca do algoritmo padrão
		String hashAntigo = "{bcrypt}" + new BCryptPasswordEncoder().encode("senha-antiga");

		assertThat(encoder.matches("senha-antiga", hashAntigo)).isTrue();
	}

}
