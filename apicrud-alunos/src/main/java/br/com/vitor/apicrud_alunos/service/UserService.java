package br.com.vitor.apicrud_alunos.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.repository.UserRepository;

// O Spring Security chama loadUserByUsername ao autenticar. Por existir este bean, o Spring Boot deixa de criar
// o usuário padrão com senha gerada no log.
@Service
public class UserService implements UserDetailsService {

	private final UserRepository repository;

	public UserService(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// O MySQL compara sem diferenciar acentos (collation utf8mb4_unicode_ci): "admín" acharia o usuário "admin".
		// Por isso o nome que veio é conferido de novo aqui, ignorando só as maiúsculas (o cadastro também as ignora).
		return repository.findByUserName(username)
				.filter(usuario -> usuario.getUsername().equalsIgnoreCase(username))
				.orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
	}

}
