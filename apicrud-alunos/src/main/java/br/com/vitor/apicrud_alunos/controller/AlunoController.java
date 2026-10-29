package br.com.vitor.apicrud_alunos.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import br.com.vitor.apicrud_alunos.dto.AlunoRequest;
import br.com.vitor.apicrud_alunos.dto.AlunoResponse;
import br.com.vitor.apicrud_alunos.dto.PageResponse;
import br.com.vitor.apicrud_alunos.mapper.AlunoMapper;
import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.service.AlunoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

	private final AlunoService service;
	private final AlunoMapper mapper;

	public AlunoController(AlunoService service, AlunoMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@PostMapping
	public ResponseEntity<AlunoResponse> criar(@Valid @RequestBody AlunoRequest request) {
		Aluno criado = service.criar(mapper.toEntity(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(criado.getId()).toUri();
		return ResponseEntity.created(location).body(mapper.toResponse(criado));
	}

	@GetMapping
	public PageResponse<AlunoResponse> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Boolean ativo,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "id") String sort) {
		return service.listar(nome, ativo, page, size, sort).map(mapper::toResponse);
	}

	@GetMapping("/{id}")
	public AlunoResponse buscar(@PathVariable Long id) {
		return mapper.toResponse(service.buscar(id));
	}

	@PutMapping("/{id}")
	public AlunoResponse atualizar(@PathVariable Long id, @Valid @RequestBody AlunoRequest request) {
		return mapper.toResponse(service.atualizar(id, mapper.toEntity(request)));
	}

	@PatchMapping("/{id}/disable")
	public AlunoResponse desativar(@PathVariable Long id) {
		return mapper.toResponse(service.desativar(id));
	}

	@PatchMapping("/{id}/enable")
	public AlunoResponse ativar(@PathVariable Long id) {
		return mapper.toResponse(service.ativar(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

}
