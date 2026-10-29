package br.com.vitor.apicrud_alunos.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.com.vitor.apicrud_alunos.dto.PageResponse;
import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.exception.ResourceNotFoundException;
import br.com.vitor.apicrud_alunos.model.Aluno;

// CRUD mockado: os alunos ficam em memória até o repository entrar
@Service
public class AlunoService {

	private static final Logger log = LoggerFactory.getLogger(AlunoService.class);

	private static final int TAMANHO_MAXIMO_PAGINA = 100;

	private static final Map<String, Comparator<Aluno>> ORDENACOES = Map.of(
			"id", Comparator.comparing(Aluno::getId),
			"nome", Comparator.comparing(Aluno::getNome, String.CASE_INSENSITIVE_ORDER),
			"email", Comparator.comparing(Aluno::getEmail, String.CASE_INSENSITIVE_ORDER),
			"dataNascimento", Comparator.comparing(Aluno::getDataNascimento));

	private final Map<Long, Aluno> alunos = new LinkedHashMap<>();
	private long proximoId = 1;

	public synchronized Aluno criar(Aluno novo) {
		validarEmailDisponivel(novo.getEmail(), null);
		novo.setId(proximoId++);
		novo.setAtivo(true);
		alunos.put(novo.getId(), novo);
		log.info("POST /alunos - aluno criado com id={}", novo.getId());
		return novo;
	}

	public synchronized PageResponse<Aluno> listar(String nome, Boolean ativo, int page, int size, String sort) {
		if (page < 0) {
			throw new InvalidRequestException("O parâmetro page deve ser maior ou igual a 0");
		}
		if (size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
			throw new InvalidRequestException("O parâmetro size deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA);
		}
		Comparator<Aluno> ordenacao = ordenacao(sort);
		String filtroNome = nome == null ? "" : nome.trim().toLowerCase(Locale.ROOT);

		List<Aluno> filtrados = alunos.values().stream()
				.filter(a -> a.getNome().toLowerCase(Locale.ROOT).contains(filtroNome))
				.filter(a -> ativo == null || a.isAtivo() == ativo)
				.sorted(ordenacao.thenComparing(Aluno::getId))
				.toList();

		int total = filtrados.size();
		int inicio = (int) Math.min((long) page * size, total);
		int fim = Math.min(inicio + size, total);
		int totalPaginas = (total + size - 1) / size;
		return new PageResponse<>(filtrados.subList(inicio, fim), page, size, total, totalPaginas);
	}

	public synchronized Aluno buscar(Long id) {
		validarId(id);
		Aluno aluno = alunos.get(id);
		if (aluno == null) {
			throw new ResourceNotFoundException("Aluno não encontrado com id=" + id);
		}
		return aluno;
	}

	public synchronized Aluno atualizar(Long id, Aluno dados) {
		Aluno aluno = buscar(id);
		validarEmailDisponivel(dados.getEmail(), id);
		aluno.setNome(dados.getNome());
		aluno.setEmail(dados.getEmail());
		aluno.setDataNascimento(dados.getDataNascimento());
		log.info("PUT /alunos/{} - aluno atualizado", id);
		return aluno;
	}

	public synchronized Aluno desativar(Long id) {
		Aluno aluno = buscar(id);
		aluno.setAtivo(false);
		log.info("PATCH /alunos/{}/disable - aluno desativado", id);
		return aluno;
	}

	public synchronized Aluno ativar(Long id) {
		Aluno aluno = buscar(id);
		aluno.setAtivo(true);
		log.info("PATCH /alunos/{}/enable - aluno ativado", id);
		return aluno;
	}

	public synchronized void remover(Long id) {
		buscar(id);
		alunos.remove(id);
		log.info("DELETE /alunos/{} - aluno removido", id);
	}

	private void validarId(Long id) {
		if (id == null || id <= 0) {
			throw new InvalidRequestException("O id deve ser um número inteiro positivo");
		}
	}

	private void validarEmailDisponivel(String email, Long idAtual) {
		boolean emUso = alunos.values().stream()
				.anyMatch(a -> !a.getId().equals(idAtual) && a.getEmail().equalsIgnoreCase(email));
		if (emUso) {
			throw new ConflictException("Já existe um aluno cadastrado com este e-mail");
		}
	}

	// sort no formato "campo" ou "campo,asc|desc"
	private Comparator<Aluno> ordenacao(String sort) {
		String[] partes = sort.split(",");
		Comparator<Aluno> ordenacao = ORDENACOES.get(partes[0].trim());
		if (ordenacao == null) {
			throw new InvalidRequestException("Não é possível ordenar por '" + partes[0].trim()
					+ "'. Campos aceitos: " + String.join(", ", new TreeSet<>(ORDENACOES.keySet())));
		}
		if (partes.length == 1 || partes[1].trim().equalsIgnoreCase("asc")) {
			return ordenacao;
		}
		if (partes.length == 2 && partes[1].trim().equalsIgnoreCase("desc")) {
			return ordenacao.reversed();
		}
		throw new InvalidRequestException("O parâmetro sort deve ter o formato campo ou campo,asc ou campo,desc");
	}

}
