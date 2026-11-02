package br.com.vitor.apicrud_alunos.assembler;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import br.com.vitor.apicrud_alunos.controller.AlunoController;
import br.com.vitor.apicrud_alunos.dto.AlunoResponse;

// Transforma um AlunoResponse em um modelo HAL, com os links do que o cliente pode fazer a seguir
@Component
public class AlunoModelAssembler implements RepresentationModelAssembler<AlunoResponse, EntityModel<AlunoResponse>> {

	@Override
	public EntityModel<AlunoResponse> toModel(AlunoResponse aluno) {
		EntityModel<AlunoResponse> modelo = EntityModel.of(aluno,
				linkTo(methodOn(AlunoController.class).buscar(aluno.id())).withSelfRel(),
				linkTo(AlunoController.class).withRel("alunos"));
		// só oferece a transição que faz sentido para o estado atual do aluno.
		// Aqui o caminho é montado com slash(): o methodOn() cria um proxy do tipo de retorno do método, e
		// desativar()/ativar() devolvem AlunoResponse, um record (final), que não pode ser proxy.
		Link base = linkTo(AlunoController.class).slash(aluno.id()).withSelfRel();
		if (aluno.ativo()) {
			modelo.add(Link.of(base.getHref() + "/disable", "disable"));
		} else {
			modelo.add(Link.of(base.getHref() + "/enable", "enable"));
		}
		return modelo;
	}

}
