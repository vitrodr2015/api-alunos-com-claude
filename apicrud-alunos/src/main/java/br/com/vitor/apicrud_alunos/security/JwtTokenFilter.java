package br.com.vitor.apicrud_alunos.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Lê "Authorization: Bearer <token>" a cada requisição e, se o token for válido, autentica quem chamou.
// Se o cabeçalho não existir ou o token for inválido, a requisição segue SEM autenticação: as rotas protegidas
// respondem 401 pelo RestAuthenticationEntryPoint, e as públicas continuam funcionando.
// Esta classe NÃO é um @Component de propósito: um filtro que é bean do Spring é registrado também fora da cadeia
// do Spring Security e passaria a rodar duas vezes. Quem o cria e o coloca na cadeia é a SecurityConfig.
public class JwtTokenFilter extends OncePerRequestFilter {

	private static final String PREFIXO = "Bearer ";

	private final JwtTokenProvider tokenProvider;

	public JwtTokenFilter(JwtTokenProvider tokenProvider) {
		this.tokenProvider = tokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
		// o nome do esquema não diferencia maiúsculas de minúsculas (RFC 6750)
		if (cabecalho != null && cabecalho.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
			String token = cabecalho.substring(PREFIXO.length()).trim();
			try {
				Authentication autenticacao = tokenProvider.getAuthentication(token);
				SecurityContext contexto = SecurityContextHolder.createEmptyContext();
				contexto.setAuthentication(autenticacao);
				SecurityContextHolder.setContext(contexto);
			} catch (InvalidTokenException e) {
				SecurityContextHolder.clearContext();
			}
		}
		chain.doFilter(request, response);
	}

}
