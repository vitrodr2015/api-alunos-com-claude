package br.com.vitor.apicrud_alunos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

@SpringBootApplication
public class ApicrudAlunosApplication {

	public static void main(String[] args) {
		SpringApplication aplicacao = new SpringApplication(ApicrudAlunosApplication.class);
		// Segredos sem valor padrão: sem a variável, a aplicação para logo na subida, com mensagem clara.
		// DB_PASSWORD: o Spring Boot não falha quando ${DB_PASSWORD} não existe, ele manda o texto literal como
		// senha e o erro aparece só como "Access denied". JWT_SECRET: a mesma regra (o tamanho mínimo é conferido
		// pelo JwtTokenProvider). Fica no main (e não em um bean) para rodar antes de o Hibernate tentar conectar;
		// os testes não passam por aqui (usam o H2 e o segredo falso do application.yml de teste).
		aplicacao.addListeners((ApplicationEnvironmentPreparedEvent evento) -> {
			var ambiente = evento.getEnvironment();
			if (ambiente.getProperty("spring.datasource.url") != null
					&& ambiente.getProperty("DB_PASSWORD", "").isBlank()) {
				throw new IllegalStateException(
						"A variável de ambiente DB_PASSWORD não foi definida. Defina a senha do banco antes de subir a aplicação.");
			}
			if (ambiente.getProperty("JWT_SECRET", "").isBlank()) {
				throw new IllegalStateException(
						"A variável de ambiente JWT_SECRET não foi definida. Defina um segredo de pelo menos 32 caracteres aleatórios antes de subir a aplicação.");
			}
		});
		aplicacao.run(args);
	}

}
