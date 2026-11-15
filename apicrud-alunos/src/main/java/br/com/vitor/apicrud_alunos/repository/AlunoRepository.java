package br.com.vitor.apicrud_alunos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.vitor.apicrud_alunos.model.Aluno;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

	// nome: "começa com", sem diferenciar maiúsculas de minúsculas (o Spring Data escapa % e _ digitados).
	// "ana" acha Ana Souza, mas não Mariana nem Tatiana; texto vazio acha todos.
	Page<Aluno> findByNomeStartingWithIgnoreCase(String nome, Pageable pageable);

	Page<Aluno> findByNomeStartingWithIgnoreCaseAndAtivo(String nome, boolean ativo, Pageable pageable);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	// Apaga numa só instrução e devolve quantas linhas apagou (0 = o aluno não existia, ou outra requisição já o apagou).
	// Atômico: não há "ler e depois apagar", que dava erro 500 quando dois DELETE do mesmo aluno chegavam juntos.
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Aluno a where a.id = :id")
	int apagarPorId(@Param("id") Long id);

}
