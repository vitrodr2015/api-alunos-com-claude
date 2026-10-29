package br.com.vitor.apicrud_alunos.mapper;

import org.springframework.stereotype.Component;

import br.com.vitor.apicrud_alunos.dto.AlunoRequest;
import br.com.vitor.apicrud_alunos.dto.AlunoResponse;
import br.com.vitor.apicrud_alunos.model.Aluno;

@Component
public class AlunoMapper {

	public Aluno toEntity(AlunoRequest request) {
		Aluno aluno = new Aluno();
		aluno.setNome(request.nome().trim());
		aluno.setEmail(request.email().trim());
		aluno.setDataNascimento(request.dataNascimento());
		return aluno;
	}

	public AlunoResponse toResponse(Aluno aluno) {
		return new AlunoResponse(aluno.getId(), aluno.getNome(), aluno.getEmail(), aluno.getDataNascimento(),
				aluno.isAtivo());
	}

}
