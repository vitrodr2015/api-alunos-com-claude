package br.com.vitor.apicrud_alunos.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;

// Base dos testes de INTEGRACAO: a aplicacao inteira (servidor de verdade, filtros ligados) contra um MySQL 8.4 REAL.
// - UM conteiner para todas as classes de integracao (padrao "singleton container"): sobe uma vez, na primeira classe que
//   precisar, e o Ryuk (do Testcontainers) o remove quando a JVM termina. Por isso nao ha @Container nem @Testcontainers.
// - O perfil "testcontainers" (application-testcontainers.yml) liga o Flyway e poe ddl-auto em validate: o banco nasce so
//   das migrations V1 a V6, como em producao.
// - O primeiro administrador nasce pelo mesmo caminho da producao (AdminBootstrapRunner), com uma senha FALSA de teste.
// Exige o Docker em execucao (no computador e na esteira).
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"app.admin-bootstrap.username=" + AbstractIntegrationTest.ADMIN,
		"app.admin-bootstrap.password=" + AbstractIntegrationTest.SENHA_ADMIN })
@ActiveProfiles("testcontainers")
public abstract class AbstractIntegrationTest {

	protected static final String ADMIN = "admin.integracao";
	protected static final String SENHA_ADMIN = "Senha-Integracao-Nao-E-Real-123";

	@ServiceConnection
	protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

	static {
		MYSQL.start();
	}

}
