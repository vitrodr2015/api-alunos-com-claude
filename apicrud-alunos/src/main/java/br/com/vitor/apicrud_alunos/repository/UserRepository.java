package br.com.vitor.apicrud_alunos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.vitor.apicrud_alunos.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUserName(String userName);

	// o nome de usuário é único sem diferenciar maiúsculas de minúsculas (no MySQL a collation já faz isso; no H2 não)
	boolean existsByUserNameIgnoreCase(String userName);

}
