package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.vitor.apicrud_alunos.dto.PageResponse;
import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.exception.ResourceNotFoundException;
import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.repository.AlunoRepository;

// Testes unitários: o repository é simulado (Mockito), sem Spring e sem banco
@ExtendWith(MockitoExtension.class)
class AlunoServiceTests {

	@Mock
	private AlunoRepository repository;

	@InjectMocks
	private AlunoService service;

	private Aluno aluno(Long id, String nome, String email) {
		Aluno aluno = new Aluno();
		aluno.setId(id);
		aluno.setNome(nome);
		aluno.setEmail(email);
		aluno.setDataNascimento(LocalDate.of(2000, 1, 1));
		return aluno;
	}

	// ---------- criar ----------

	@Test
	void criarSalvaComoAtivoEIgnoraOIdRecebido() {
		Aluno novo = aluno(99L, "Ana Souza", "ana@escola.com");
		novo.setAtivo(false);
		when(repository.existsByEmailIgnoreCase("ana@escola.com")).thenReturn(false);
		when(repository.save(any(Aluno.class))).thenAnswer(i -> i.getArgument(0));

		Aluno criado = service.criar(novo);

		ArgumentCaptor<Aluno> salvo = ArgumentCaptor.forClass(Aluno.class);
		verify(repository).save(salvo.capture());
		assertThat(salvo.getValue().getId()).isNull();
		assertThat(criado.isAtivo()).isTrue();
	}

	@Test
	void criarComEmailEmUsoLancaConflito() {
		when(repository.existsByEmailIgnoreCase("ana@escola.com")).thenReturn(true);

		assertThatThrownBy(() -> service.criar(aluno(null, "Ana Souza", "ana@escola.com")))
				.isInstanceOf(ConflictException.class)
				.hasMessage("Já existe um aluno cadastrado com este e-mail");
		verify(repository, never()).save(any());
	}

	// ---------- buscar ----------

	@Test
	void buscarDevolveOAluno() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));

		assertThat(service.buscar(1L)).isSameAs(existente);
	}

	@Test
	void buscarInexistenteLancaNaoEncontrado() {
		when(repository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.buscar(7L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessage("Aluno não encontrado com id=7");
	}

	@Test
	void buscarComIdInvalidoLancaRequisicaoInvalidaSemConsultarOBanco() {
		for (Long invalido : new Long[] { null, 0L, -1L }) {
			assertThatThrownBy(() -> service.buscar(invalido))
					.isInstanceOf(InvalidRequestException.class)
					.hasMessage("O id deve ser um número inteiro positivo");
		}
		verify(repository, never()).findById(any());
	}

	// ---------- atualizar ----------

	@Test
	void atualizarSubstituiNomeEmailEDataMasNaoOAtivo() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		existente.setAtivo(false);
		when(repository.findById(1L)).thenReturn(Optional.of(existente));
		when(repository.existsByEmailIgnoreCaseAndIdNot("nova@escola.com", 1L)).thenReturn(false);
		when(repository.save(existente)).thenReturn(existente);
		Aluno dados = aluno(null, "Ana Maria", "nova@escola.com");
		dados.setDataNascimento(LocalDate.of(1999, 12, 31));

		Aluno atualizado = service.atualizar(1L, dados);

		assertThat(atualizado.getNome()).isEqualTo("Ana Maria");
		assertThat(atualizado.getEmail()).isEqualTo("nova@escola.com");
		assertThat(atualizado.getDataNascimento()).isEqualTo(LocalDate.of(1999, 12, 31));
		assertThat(atualizado.isAtivo()).isFalse();
	}

	@Test
	void atualizarComEmailDeOutroAlunoLancaConflito() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));
		when(repository.existsByEmailIgnoreCaseAndIdNot("bruno@escola.com", 1L)).thenReturn(true);

		assertThatThrownBy(() -> service.atualizar(1L, aluno(null, "Ana Souza", "bruno@escola.com")))
				.isInstanceOf(ConflictException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void atualizarInexistenteLancaNaoEncontradoAntesDeChecarOEmail() {
		when(repository.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.atualizar(5L, aluno(null, "Ana Souza", "ana@escola.com")))
				.isInstanceOf(ResourceNotFoundException.class);
		verify(repository, never()).existsByEmailIgnoreCaseAndIdNot(anyString(), any());
	}

	// ---------- desativar / ativar ----------

	@Test
	void desativarMarcaComoInativoEEhIdempotente() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));
		when(repository.save(existente)).thenReturn(existente);

		assertThat(service.desativar(1L).isAtivo()).isFalse();
		assertThat(service.desativar(1L).isAtivo()).isFalse();
	}

	@Test
	void ativarMarcaComoAtivoEEhIdempotente() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		existente.setAtivo(false);
		when(repository.findById(1L)).thenReturn(Optional.of(existente));
		when(repository.save(existente)).thenReturn(existente);

		assertThat(service.ativar(1L).isAtivo()).isTrue();
		assertThat(service.ativar(1L).isAtivo()).isTrue();
	}

	@Test
	void desativarInexistenteLancaNaoEncontrado() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.desativar(9L)).isInstanceOf(ResourceNotFoundException.class);
	}

	// ---------- remover ----------

	@Test
	void removerApagaOAluno() {
		Aluno existente = aluno(1L, "Ana Souza", "ana@escola.com");
		when(repository.findById(1L)).thenReturn(Optional.of(existente));

		service.remover(1L);

		verify(repository).delete(existente);
	}

	@Test
	void removerInexistenteLancaNaoEncontradoSemApagar() {
		when(repository.findById(3L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.remover(3L)).isInstanceOf(ResourceNotFoundException.class);
		verify(repository, never()).delete(any());
	}

	// ---------- listar ----------

	@Test
	void listarMontaAPaginaEOrdenaPorNomeSemDiferenciarCaixaComDesempatePeloId() {
		Page<Aluno> pagina = new PageImpl<>(List.of(aluno(1L, "Ana", "a@x.com")), org.springframework.data.domain.PageRequest.of(1, 5), 11);
		when(repository.buscar(eq("souza"), eq(true), any(Pageable.class))).thenReturn(pagina);

		PageResponse<Aluno> resposta = service.listar("  souza ", true, 1, 5, "nome,desc");

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).buscar(eq("souza"), eq(true), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
		List<Sort.Order> ordens = pageable.getValue().getSort().toList();
		assertThat(ordens).hasSize(2);
		assertThat(ordens.get(0).getProperty()).isEqualTo("nome");
		assertThat(ordens.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(ordens.get(0).isIgnoreCase()).isTrue();
		assertThat(ordens.get(1).getProperty()).isEqualTo("id");
		assertThat(resposta.totalElements()).isEqualTo(11);
		assertThat(resposta.totalPages()).isEqualTo(3);
		assertThat(resposta.page()).isEqualTo(1);
		assertThat(resposta.size()).isEqualTo(5);
	}

	@Test
	void listarSemFiltrosUsaNomeVazioEAtivoNulo() {
		when(repository.buscar(eq(""), eq(null), any(Pageable.class))).thenReturn(Page.empty());

		PageResponse<Aluno> resposta = service.listar(null, null, 0, 10, "id");

		assertThat(resposta.content()).isEmpty();
		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).buscar(eq(""), eq(null), pageable.capture());
		assertThat(pageable.getValue().getSort().toList()).hasSize(1);
	}

	@Test
	void listarComParametrosInvalidosLancaRequisicaoInvalida() {
		assertThatThrownBy(() -> service.listar(null, null, -1, 10, "id"))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.listar(null, null, 0, 0, "id"))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.listar(null, null, 0, 101, "id"))
				.isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.listar(null, null, 0, 10, "senha"))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("Campos aceitos: dataNascimento, email, id, nome");
		assertThatThrownBy(() -> service.listar(null, null, 0, 10, "nome,sideways"))
				.isInstanceOf(InvalidRequestException.class);
		verify(repository, never()).buscar(any(), any(), any());
	}

}
