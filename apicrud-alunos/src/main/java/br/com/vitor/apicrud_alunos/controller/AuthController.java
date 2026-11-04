package br.com.vitor.apicrud_alunos.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.vitor.apicrud_alunos.dto.RefreshRequest;
import br.com.vitor.apicrud_alunos.dto.RegisterRequest;
import br.com.vitor.apicrud_alunos.dto.SignInRequest;
import br.com.vitor.apicrud_alunos.dto.TokenResponse;
import br.com.vitor.apicrud_alunos.dto.UserResponse;
import br.com.vitor.apicrud_alunos.service.AuthService;
import jakarta.validation.Valid;

// Autenticação. Só JSON (nada de XML/YAML aqui: são rotas de credenciais e tokens).
// signin e refresh são públicos; register é exclusivo de ADMIN (regra na SecurityConfig).
@RestController
@RequestMapping(value = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

	private final AuthService service;

	public AuthController(AuthService service) {
		this.service = service;
	}

	@PostMapping("/signin")
	public ResponseEntity<TokenResponse> signIn(@Valid @RequestBody SignInRequest pedido) {
		return semCache(TokenResponse.from(service.signIn(pedido.username(), pedido.password())));
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest pedido) {
		return semCache(TokenResponse.from(service.refresh(pedido.refreshToken())));
	}

	@PostMapping("/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest pedido) {
		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(service.register(pedido)));
	}

	// respostas com tokens nunca devem ser guardadas em cache de navegador ou de proxy
	private static <T> ResponseEntity<T> semCache(T corpo) {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(corpo);
	}

}
