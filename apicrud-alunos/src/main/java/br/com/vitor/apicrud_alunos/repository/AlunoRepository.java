package br.com.vitor.apicrud_alunos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vitor.apicrud_alunos.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {
}
