package br.com.vitor.apicrud_alunos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.com.vitor.apicrud_alunos.security.JwtTokenFilter;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import br.com.vitor.apicrud_alunos.security.RestAccessDeniedHandler;
import br.com.vitor.apicrud_alunos.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
			RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler accessDeniedHandler) throws Exception {
		return http
				// API com token: não há cookie de sessão, então não há o que o CSRF proteger
				.csrf(AbstractHttpConfigurer::disable)
				// usa a configuração de CORS do passo 36 do playbook (ainda não existe: por ora, sem cabeçalhos de CORS)
				.cors(Customizer.withDefaults())
				// stateless: nada fica guardado no servidor entre requisições; cada uma traz o seu token
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// sem tela de login nem Basic: a única forma de entrar é o token
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				// 401 e 403 em JSON, no formato ErrorResponse
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				// REVISE esta lista antes de cada deploy: rota liberada "só para testar" e esquecida vira brecha
				.authorizeHttpRequests(rotas -> rotas
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/auth/signin", "/auth/refresh").permitAll()
						// cadastro de usuários: só quem tem a permissão ADMIN (qualquer método; sem token dá 401, sem a permissão dá 403)
						.requestMatchers("/auth/register").hasAuthority("ADMIN")
						.anyRequest().authenticated())
				.addFilterBefore(new JwtTokenFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	// Confere usuário e senha no login (/auth/signin): carrega o usuário pelo UserService e compara a senha com o hash.
	// Para usuário inexistente o Spring ainda gasta o tempo de uma comparação de hash, o que dificulta descobrir
	// quais usuários existem medindo a demora da resposta.
	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provedor = new DaoAuthenticationProvider(userDetailsService);
		provedor.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provedor);
	}

}
