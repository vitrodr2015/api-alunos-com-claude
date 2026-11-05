package br.com.vitor.apicrud_alunos.config;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import br.com.vitor.apicrud_alunos.security.Authorities;
import br.com.vitor.apicrud_alunos.security.JwtTokenFilter;
import br.com.vitor.apicrud_alunos.security.JwtTokenProvider;
import br.com.vitor.apicrud_alunos.security.RestAccessDeniedHandler;
import br.com.vitor.apicrud_alunos.security.RestAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
			RestAuthenticationEntryPoint entryPoint, RestAccessDeniedHandler accessDeniedHandler,
			@Value("${springdoc.api-docs.enabled:false}") boolean documentacaoLigada) throws Exception {
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
						// health check: público e SÓ de leitura (devolve apenas {"status":"UP"}); o resto do /actuator continua negado
						.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
						// documentação (Swagger): só é pública quando SWAGGER_ENABLED=true; desligado (o padrão) cai na regra final
						.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
								.access((autenticacao, contexto) -> new AuthorizationDecision(documentacaoLigada))
						// cadastro de usuários: só quem tem a permissão ADMIN (qualquer método; sem token dá 401, sem a permissão dá 403)
						.requestMatchers("/auth/register").hasAuthority(Authorities.ADMIN)
						// ---- autorização por rota: quem pode o quê (estar logado não basta) ----
						// leitura: USER ou ADMIN
						.requestMatchers(HttpMethod.GET, "/alunos", "/alunos/{id}").hasAnyAuthority(Authorities.USER, Authorities.ADMIN)
						.requestMatchers(HttpMethod.HEAD, "/alunos", "/alunos/{id}").hasAnyAuthority(Authorities.USER, Authorities.ADMIN)
						// escrita reversível: USER ou ADMIN (criar, editar, ativar e desativar)
						.requestMatchers(HttpMethod.POST, "/alunos").hasAnyAuthority(Authorities.USER, Authorities.ADMIN)
						.requestMatchers(HttpMethod.PUT, "/alunos/{id}").hasAnyAuthority(Authorities.USER, Authorities.ADMIN)
						.requestMatchers(HttpMethod.PATCH, "/alunos/{id}/disable", "/alunos/{id}/enable")
								.hasAnyAuthority(Authorities.USER, Authorities.ADMIN)
						// apagar é destrutivo e irreversível: só ADMIN
						.requestMatchers(HttpMethod.DELETE, "/alunos/{id}").hasAuthority(Authorities.ADMIN)
						// REGRA FINAL: o que não tem regra acima é NEGADO (e não "liberado para qualquer logado").
						// Toda rota nova nasce fechada e só abre quando alguém escrever a regra dela aqui.
						.anyRequest().denyAll())
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
