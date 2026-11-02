package br.com.vitor.apicrud_alunos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.vitor.apicrud_alunos.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	// nome: "contém", sem diferenciar maiúsculas de minúsculas (locate evita tratar % e _ como curingas);
	// ativo: opcional (nulo traz todos)
	@Query("select a from Aluno a where locate(lower(:nome), lower(a.nome)) > 0 "
			+ "and (:ativo is null or a.ativo = :ativo)")
	Page<Aluno> buscar(@Param("nome") String nome, @Param("ativo") Boolean ativo, Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

}
