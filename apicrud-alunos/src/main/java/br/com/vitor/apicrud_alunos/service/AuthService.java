package br.com.vitor.apicrud_alunos.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.vitor.apicrud_alunos.dto.RegisterRequest;
import br.com.vitor.apicrud_alunos.exception.ConflictException;
import br.com.vitor.apicrud_alunos.exception.InvalidRequestException;
import br.com.vitor.apicrud_alunos.model.Permission;
import br.com.vitor.apicrud_alunos.model.User;
import br.com.vitor.apicrud_alunos.repository.PermissionRepository;
import br.com.vitor.apicrud_alunos.repository.UserRepository;
import br.com.vitor.apicrud_alunos.security.InvalidCredentialsException;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import br.com.vitor.apicrud_alunos.security.TokenPair;

@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	// permissão que todo usuário cadastrado recebe (semeada na migration V5)
	static final String PERMISSAO_PADRAO = br.com.vitor.apicrud_alunos.security.Authorities.USER;

	// limite do BCrypt, em BYTES (letras acentuadas ocupam mais de um byte)
	static final int LIMITE_DA_SENHA_EM_BYTES = 72;

	private final AuthenticationManager authenticationManager;
	private final JwtTokenProvider tokenProvider;
	private final UserRepository userRepository;
	private final PermissionRepository permissionRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
			UserRepository userRepository, PermissionRepository permissionRepository, PasswordEncoder passwordEncoder) {
		this.authenticationManager = authenticationManager;
		this.tokenProvider = tokenProvider;
		this.userRepository = userRepository;
		this.permissionRepository = permissionRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// Login. Qualquer falha de autenticação (usuário inexistente, senha errada, conta desativada, bloqueada ou
	// expirada) vira a MESMA InvalidCredentialsException: assim a resposta não revela quais usuários existem.
	@Transactional(readOnly = true)
	public TokenPair signIn(String username, String password) {
		try {
			Authentication autenticacao = authenticationManager
					.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
			return tokenProvider.createTokenPair((UserDetails) autenticacao.getPrincipal());
		} catch (AuthenticationException e) {
			throw new InvalidCredentialsException();
		}
	}

	// Troca um refresh token válido por um par novo (a validação e a recarga do usuário estão no provedor)
	public TokenPair refresh(String refreshToken) {
		return tokenProvider.renew(refreshToken);
	}

	@Transactional
	public User register(RegisterRequest pedido) {
		if (pedido.password().getBytes(StandardCharsets.UTF_8).length > LIMITE_DA_SENHA_EM_BYTES) {
			throw new InvalidRequestException(
					"password: deve ter no máximo " + LIMITE_DA_SENHA_EM_BYTES + " bytes (letras acentuadas ocupam mais de um)");
		}
		if (userRepository.existsByUserNameIgnoreCase(pedido.username())) {
			throw new ConflictException("Já existe um usuário com este nome");
		}
		Permission padrao = permissionRepository.findByDescription(PERMISSAO_PADRAO)
				.orElseThrow(() -> new IllegalStateException("A permissão padrão " + PERMISSAO_PADRAO + " não existe no banco"));

		User usuario = new User();
		usuario.setUserName(pedido.username());
		usuario.setFullName(pedido.fullName());
		usuario.setPassword(passwordEncoder.encode(pedido.password()));
		usuario.setPermissions(new ArrayList<>(List.of(padrao)));
		User salvo = userRepository.save(usuario);
		log.info("POST /auth/register - usuário criado com id={}", salvo.getId());
		return salvo;
	}

}
