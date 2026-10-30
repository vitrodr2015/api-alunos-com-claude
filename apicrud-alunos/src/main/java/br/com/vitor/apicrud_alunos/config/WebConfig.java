package br.com.vitor.apicrud_alunos.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final MediaType APPLICATION_X_YAML = MediaType.valueOf("application/x-yaml");

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
