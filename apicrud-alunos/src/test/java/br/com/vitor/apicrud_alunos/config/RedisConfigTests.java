package br.com.vitor.apicrud_alunos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import br.com.vitor.apicrud_alunos.dto.AlunoResponse;
import br.com.vitor.apicrud_alunos.model.Aluno;

// O serializer do cache: o que vai para o Redis tem de VOLTAR como o mesmo tipo (e só tipos permitidos voltam).
// Sem Redis: testa só a ida e volta (serialize/deserialize). O Redis de verdade é testado no CacheRedisIntegrationTests.
class RedisConfigTests {

	private final RedisSerializer<Object> serializador = RedisConfig.serializador();

	private static Aluno aluno() {
		Aluno a = new Aluno();
		a.setId(7L);
		a.setNome("Ana");
		a.setEmail("ana@escola.com");
		a.setDataNascimento(LocalDate.of(2000, 1, 31));
		a.setAtivo(true);
		return a;
	}

	@Test
	void aEntidadeAlunoVoltaComoAluno_naoComoMapa() {
		Object volta = serializador.deserialize(serializador.serialize(aluno()));

		// o cache do buscar() guarda a ENTIDADE; se o pacote model não fosse permitido, isto lançaria SerializationException
		assertThat(volta).isInstanceOf(Aluno.class);
		Aluno a = (Aluno) volta;
		assertThat(a.getId()).isEqualTo(7L);
		assertThat(a.getDataNascimento()).isEqualTo(LocalDate.of(2000, 1, 31));
		assertThat(a.isAtivo()).isTrue();
	}

	@Test
	void oRecordAlunoResponseTambemVolta() {
		AlunoResponse r = new AlunoResponse(1L, "Ana", "ana@escola.com", LocalDate.of(2000, 1, 31), true);
		assertThat(serializador.deserialize(serializador.serialize(r))).isEqualTo(r);
	}

	@Test
	void listaDeAlunosVoltaComoListaDeAlunos() {
		List<Aluno> lista = new ArrayList<>(List.of(aluno()));
		Object volta = serializador.deserialize(serializador.serialize(lista));
		assertThat(volta).isInstanceOf(List.class);
		assertThat(((List<?>) volta).get(0)).isInstanceOf(Aluno.class);
	}

	@Test
	void oJsonGravadoLevaOTipoParaPoderVoltar() {
		String json = new String(serializador.serialize(aluno()), StandardCharsets.UTF_8);
		assertThat(json).contains("\"@class\":\"br.com.vitor.apicrud_alunos.model.Aluno\"")
				.contains("\"dataNascimento\":\"2000-01-31\"");
	}

	@Test
	void tipoForaDaListaPermitidaNaoEReconstruido() {
		// um valor adulterado no Redis não pode fazer o Jackson criar uma classe qualquer
		byte[] adulterado = "{\"@class\":\"java.io.File\",\"path\":\"/tmp/x\"}".getBytes(StandardCharsets.UTF_8);
		assertThatThrownBy(() -> serializador.deserialize(adulterado)).isInstanceOf(SerializationException.class);
	}
}
