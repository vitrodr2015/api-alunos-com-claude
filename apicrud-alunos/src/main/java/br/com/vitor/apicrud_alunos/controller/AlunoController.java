package br.com.vitor.apicrud_alunos.controller;

import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
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

import br.com.vitor.apicrud_alunos.assembler.AlunoModelAssembler;
import br.com.vitor.apicrud_alunos.dto.AlunoRequest;
import br.com.vitor.apicrud_alunos.dto.AlunoResponse;
import br.com.vitor.apicrud_alunos.mapper.AlunoMapper;
import br.com.vitor.apicrud_alunos.model.Aluno;
import br.com.vitor.apicrud_alunos.service.AlunoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/alunos")
public class AlunoController {

	private final AlunoService service;
	private final AlunoMapper mapper;
	private final AlunoModelAssembler assembler;
	private final PagedResourcesAssembler<AlunoResponse> pagedAssembler;

	public AlunoController(AlunoService service, AlunoMapper mapper, AlunoModelAssembler assembler,
			PagedResourcesAssembler<AlunoResponse> pagedAssembler) {
		this.service = service;
		this.mapper = mapper;
		this.assembler = assembler;
		this.pagedAssembler = pagedAssembler;
	}

	@PostMapping
	public ResponseEntity<AlunoResponse> criar(@Valid @RequestBody AlunoRequest request) {
		Aluno criado = service.criar(mapper.toEntity(request));
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(criado.getId()).toUri();
		return ResponseEntity.created(location).body(mapper.toResponse(criado));
	}

	// A listagem e a busca por id respondem em HAL (JSON com _links); os demais endpoints continuam como antes
	@GetMapping(produces = { HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public PagedModel<EntityModel<AlunoResponse>> listar(
			@RequestParam(required = false) String nome,
			@RequestParam(required = false) Boolean ativo,
			Pageable pageable) {
		// page, size e sort (ex.: sort=nome,desc) são lidos pelo Spring; padrão e máximo de size ficam no application.yml
		Page<AlunoResponse> pagina = service.listar(nome, ativo, pageable).map(mapper::toResponse);
		return pagedAssembler.toModel(pagina, assembler);
	}

	@GetMapping(value = "/{id}", produces = { HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	public EntityModel<AlunoResponse> buscar(@PathVariable Long id) {
		return assembler.toModel(mapper.toResponse(service.buscar(id)));
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
