package br.com.vitor.apicrud_alunos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
		when(repository.apagarPorId(1L)).thenReturn(1);

		service.remover(1L);

		verify(repository).apagarPorId(1L);
	}

	@Test
	void removerInexistenteLancaNaoEncontrado() {
		when(repository.apagarPorId(3L)).thenReturn(0);

		assertThatThrownBy(() -> service.remover(3L)).isInstanceOf(ResourceNotFoundException.class);
	}

	// QA: dois DELETE do mesmo aluno ao mesmo tempo davam 500. Agora o segundo acha 0 linha apagada e responde 404.
	@Test
	void removerRepetidoDaNaoEncontradoNaSegundaVezEnaoErroInterno() {
		when(repository.apagarPorId(5L)).thenReturn(1, 0);

		service.remover(5L);

		assertThatThrownBy(() -> service.remover(5L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void removerComIdInvalidoNemChegaAoBanco() {
		for (Long id : new Long[] { 0L, -1L, null }) {
			assertThatThrownBy(() -> service.remover(id)).isInstanceOf(InvalidRequestException.class);
		}
		org.mockito.Mockito.verifyNoInteractions(repository);
	}

	// ---------- listar: ordenação só por campos do aluno ----------

	// QA: sort com ";" ou espaço dava 500. Agora é 400, e o banco nem é consultado.
	@Test
	void listarComOrdenacaoPorCampoDesconhecidoLancaRequisicaoInvalidaSemConsultarOBanco() {
		for (String sort : new String[] { "senha", "password", "nome;drop table aluno", "nome desc", "nome.length", "id;select 1", "'", "nome)" }) {
			assertThatThrownBy(() -> service.listar(null, null, PageRequest.of(0, 20, Sort.by(sort))))
					.as("sort=%s", sort).isInstanceOf(InvalidRequestException.class)
					.hasMessageContaining("Campos aceitos");
		}
		org.mockito.Mockito.verifyNoInteractions(repository);
	}

	@Test
	void listarAceitaOrdenarPorCadaCampoDoAluno() {
		when(repository.findByNomeContainingIgnoreCase(anyString(), any(Pageable.class))).thenReturn(Page.empty());
		for (String campo : new String[] { "id", "nome", "email", "dataNascimento", "ativo" }) {
			service.listar(null, null, PageRequest.of(0, 20, Sort.by(campo)));
		}
		verify(repository, org.mockito.Mockito.times(5)).findByNomeContainingIgnoreCase(anyString(), any(Pageable.class));
	}

	// ---------- listar ----------

	@Test
	void listarSemAtivoUsaOFiltroSoPorNomeEPassaPaginaEOrdenacaoComDesempatePeloId() {
		Page<Aluno> pagina = new PageImpl<>(List.of(aluno(1L, "Ana", "a@x.com")), PageRequest.of(1, 5), 11);
		when(repository.findByNomeContainingIgnoreCase(eq("souza"), any(Pageable.class))).thenReturn(pagina);

		Page<Aluno> resposta = service.listar("  souza ", null, PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "nome")));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByNomeContainingIgnoreCase(eq("souza"), pageable.capture());
		verify(repository, never()).findByNomeContainingIgnoreCaseAndAtivo(any(), anyBoolean(), any());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
		List<Sort.Order> ordens = pageable.getValue().getSort().toList();
		assertThat(ordens).hasSize(2);
		assertThat(ordens.get(0).getProperty()).isEqualTo("nome");
		assertThat(ordens.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(ordens.get(1).getProperty()).isEqualTo("id");
		assertThat(resposta.getTotalElements()).isEqualTo(11);
		assertThat(resposta.getTotalPages()).isEqualTo(3);
		assertThat(resposta.getNumber()).isEqualTo(1);
		assertThat(resposta.getSize()).isEqualTo(5);
	}

	@Test
	void listarComAtivoUsaOFiltroPorNomeEAtivo() {
		when(repository.findByNomeContainingIgnoreCaseAndAtivo(eq(""), eq(false), any(Pageable.class)))
				.thenReturn(Page.empty());

		Page<Aluno> resposta = service.listar(null, false, PageRequest.of(0, 20));

		assertThat(resposta.getContent()).isEmpty();
		verify(repository, never()).findByNomeContainingIgnoreCase(any(), any());
	}

	@Test
	void listarSemOrdenacaoOrdenaPeloId() {
		when(repository.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class))).thenReturn(Page.empty());

		service.listar(null, null, PageRequest.of(0, 20));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByNomeContainingIgnoreCase(eq(""), pageable.capture());
		assertThat(pageable.getValue().getSort().toList()).hasSize(1);
		assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
	}

	@Test
	void listarOrdenandoPeloIdNaoDuplicaOId() {
		when(repository.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class))).thenReturn(Page.empty());

		service.listar(null, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(repository).findByNomeContainingIgnoreCase(eq(""), pageable.capture());
		List<Sort.Order> ordens = pageable.getValue().getSort().toList();
		assertThat(ordens).hasSize(1);
		assertThat(ordens.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
	}

}
