package br.com.vitor.apicrud_alunos.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final MediaType APPLICATION_X_YAML = MediaType.valueOf("application/x-yaml");

	// origem = esquema + domínio + porta opcional, SEM barra no fim e SEM caminho (é assim que o navegador a envia)
	private static final Pattern ORIGEM_VALIDA = Pattern.compile("^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?$");

	private final List<String> origensPermitidas;

	// cors.allowed-origins (variável CORS_ALLOWED_ORIGINS): origens, separadas por vírgula, que podem chamar a API
	// pelo navegador. Vazio (o padrão) = nenhuma: sem CORS. É um padrão seguro, por isso pode ter valor padrão
	// (diferente de um segredo).
	public WebConfig(@Value("${cors.allowed-origins:}") List<String> origens) {
		this.origensPermitidas = validar(origens);
	}

	static List<String> validar(List<String> origens) {
		List<String> validas = new ArrayList<>();
		for (String origem : origens) {
			String limpa = origem == null ? "" : origem.trim();
			if (limpa.isEmpty()) {
				continue;
			}
			if (limpa.contains("*")) {
				throw new IllegalStateException("CORS_ALLOWED_ORIGINS não pode usar '*': liste só as origens "
						+ "(domínios) que realmente chamam esta API pelo navegador.");
			}
			if (!ORIGEM_VALIDA.matcher(limpa).matches()) {
				throw new IllegalStateException("Origem de CORS inválida: '" + limpa
						+ "'. Use o formato https://dominio[:porta], sem barra no fim e sem caminho.");
			}
			validas.add(limpa);
		}
		return List.copyOf(validas);
	}

	// CORS: só as origens configuradas, só os métodos e cabeçalhos que a API usa, e sem cookies (a autenticação é
	// pelo cabeçalho Authorization). Com Spring Security isto só funciona porque a SecurityConfig liga .cors() e libera
	// OPTIONS: sem isso o preflight seria barrado (401) antes de chegar aqui.
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (origensPermitidas.isEmpty()) {
			return;
		}
		registry.addMapping("/**")
				.allowedOrigins(origensPermitidas.toArray(String[]::new))
				.allowedMethods("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("Authorization", "Content-Type", "Accept")
				// o navegador só deixa o JavaScript ler cabeçalhos "seguros"; o Location do POST /alunos precisa ser exposto
				.exposedHeaders("Location")
				.allowCredentials(false)
				.maxAge(3600);
	}

	// Sem Accept, a resposta é JSON; com Accept, o formato vem do cabeçalho (json, xml ou yaml)
	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	// O Spring já traz o conversor de YAML (application/yaml); aqui ele passa a aceitar também application/x-yaml
	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.stream()
				.filter(JacksonYamlHttpMessageConverter.class::isInstance)
				.map(JacksonYamlHttpMessageConverter.class::cast)
				.forEach(yaml -> yaml.setSupportedMediaTypes(List.of(MediaType.APPLICATION_YAML, APPLICATION_X_YAML)));
	}

}
