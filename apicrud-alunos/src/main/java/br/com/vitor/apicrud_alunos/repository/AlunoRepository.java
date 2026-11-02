package br.com.vitor.apicrud_alunos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vitor.apicrud_alunos.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	// nome: "contém", sem diferenciar maiúsculas de minúsculas (o Spring Data escapa % e _ digitados)
	Page<Aluno> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

	Page<Aluno> findByNomeContainingIgnoreCaseAndAtivo(String nome, boolean ativo, Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
