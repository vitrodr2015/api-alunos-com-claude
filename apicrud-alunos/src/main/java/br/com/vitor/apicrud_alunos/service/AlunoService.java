package br.com.vitor.apicrud_alunos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.exception.ResourceNotFoundException;
import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.repository.AlunoRepository;

@Service
public class AlunoService {

	private static final Logger log = LoggerFactory.getLogger(AlunoService.class);

	private final AlunoRepository repository;

	public AlunoService(AlunoRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public Aluno criar(Aluno novo) {
		validarEmailDisponivel(novo.getEmail(), null);
		novo.setId(null);
		novo.setAtivo(true);
		Aluno salvo = repository.save(novo);
		log.info("POST /alunos - aluno criado com id={}", salvo.getId());
		return salvo;
	}

	@Transactional(readOnly = true)
	// page, size e sort chegam já resolvidos pelo Spring (padrão e máximo vêm do application.yml);
	// ordenar por um campo que não existe lança PropertyReferenceException, tratada como 400
	public Page<Aluno> listar(String nome, Boolean ativo, Pageable pageable) {
		String filtroNome = nome == null ? "" : nome.trim();
		Pageable ordenado = comDesempatePeloId(pageable);

		return ativo == null
				? repository.findByNomeContainingIgnoreCase(filtroNome, ordenado)
				: repository.findByNomeContainingIgnoreCaseAndAtivo(filtroNome, ativo, ordenado);
	}

	@Transactional(readOnly = true)
	public Aluno buscar(Long id) {
		validarId(id);
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com id=" + id));
	}

	@Transactional
	public Aluno atualizar(Long id, Aluno dados) {
		Aluno aluno = buscar(id);
		validarEmailDisponivel(dados.getEmail(), id);
		aluno.setNome(dados.getNome());
		aluno.setEmail(dados.getEmail());
		aluno.setDataNascimento(dados.getDataNascimento());
		Aluno salvo = repository.save(aluno);
		log.info("PUT /alunos/{} - aluno atualizado", id);
		return salvo;
	}

	@Transactional
	public Aluno desativar(Long id) {
		Aluno aluno = buscar(id);
		aluno.setAtivo(false);
		Aluno salvo = repository.save(aluno);
		log.info("PATCH /alunos/{}/disable - aluno desativado", id);
		return salvo;
	}

	@Transactional
	public Aluno ativar(Long id) {
		Aluno aluno = buscar(id);
		aluno.setAtivo(true);
		Aluno salvo = repository.save(aluno);
		log.info("PATCH /alunos/{}/enable - aluno ativado", id);
		return salvo;
	}

	@Transactional
	public void remover(Long id) {
		repository.delete(buscar(id));
		log.info("DELETE /alunos/{} - aluno removido", id);
	}

	private void validarId(Long id) {
		if (id == null || id <= 0) {
			throw new InvalidRequestException("O id deve ser um número inteiro positivo");
		}
	}

	// idAtual é nulo na criação; na atualização o próprio aluno não conta como conflito
	private void validarEmailDisponivel(String email, Long idAtual) {
		boolean emUso = idAtual == null
				? repository.existsByEmailIgnoreCase(email)
				: repository.existsByEmailIgnoreCaseAndIdNot(email, idAtual);
		if (emUso) {
			throw new ConflictException("Já existe um aluno cadastrado com este e-mail");
		}
	}

	// o id desempata a ordenação (e é a ordem padrão), para a paginação ser estável entre as páginas
	private Pageable comDesempatePeloId(Pageable pageable) {
		Sort sort = pageable.getSort();
		if (sort.isUnsorted()) {
			sort = Sort.by("id");
		} else if (sort.getOrderFor("id") == null) {
			sort = sort.and(Sort.by("id"));
		}
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

}
