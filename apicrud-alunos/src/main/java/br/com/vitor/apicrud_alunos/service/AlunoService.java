package br.com.vitor.apicrud_alunos.service;

import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.dto.PageResponse;
import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.exception.ResourceNotFoundException;
import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.repository.AlunoRepository;

@Service
public class AlunoService {

	private static final Logger log = LoggerFactory.getLogger(AlunoService.class);

	private static final int TAMANHO_MAXIMO_PAGINA = 100;

	// campos pelos quais a lista pode ser ordenada (nomes das propriedades da entidade)
	private static final Set<String> CAMPOS_ORDENAVEIS = Set.of("id", "nome", "email", "dataNascimento");

	// ordenar por texto sem diferenciar maiúsculas de minúsculas
	private static final Set<String> CAMPOS_DE_TEXTO = Set.of("nome", "email");

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
	public PageResponse<Aluno> listar(String nome, Boolean ativo, int page, int size, String sort) {
		if (page < 0) {
			throw new InvalidRequestException("O parâmetro page deve ser maior ou igual a 0");
		}
		if (size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
			throw new InvalidRequestException("O parâmetro size deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA);
		}
		Sort ordenacao = ordenacao(sort);
		String filtroNome = nome == null ? "" : nome.trim();

		Page<Aluno> pagina = repository.buscar(filtroNome, ativo, PageRequest.of(page, size, ordenacao));
		return new PageResponse<>(pagina.getContent(), page, size, pagina.getTotalElements(), pagina.getTotalPages());
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

	// sort no formato "campo" ou "campo,asc|desc"; o id desempata, para a paginação ser estável
	private Sort ordenacao(String sort) {
		String[] partes = sort.split(",");
		String campo = partes[0].trim();
		if (!CAMPOS_ORDENAVEIS.contains(campo)) {
			throw new InvalidRequestException("Não é possível ordenar por '" + campo
					+ "'. Campos aceitos: " + String.join(", ", new TreeSet<>(CAMPOS_ORDENAVEIS)));
		}
		Sort.Direction direcao;
		if (partes.length == 1 || partes[1].trim().equalsIgnoreCase("asc")) {
			direcao = Sort.Direction.ASC;
		} else if (partes.length == 2 && partes[1].trim().equalsIgnoreCase("desc")) {
			direcao = Sort.Direction.DESC;
		} else {
			throw new InvalidRequestException("O parâmetro sort deve ter o formato campo ou campo,asc ou campo,desc");
		}
		Sort.Order ordem = new Sort.Order(direcao, campo);
		if (CAMPOS_DE_TEXTO.contains(campo)) {
			ordem = ordem.ignoreCase();
		}
		return campo.equals("id") ? Sort.by(ordem) : Sort.by(ordem, Sort.Order.asc("id"));
	}

}
