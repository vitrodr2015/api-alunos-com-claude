package br.com.vitor.apicrud_alunos.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// HIGIENE dos arquivos versionados: nenhum segredo com valor, nenhum login conhecido que vá parar num ambiente
// publicado. Lê os ARQUIVOS REAIS do projeto (não os de teste); roda sem Spring e sem banco.
class ConfigHygieneTests {

	private static final Path RAIZ = Path.of(".");
	private static final Path APPLICATION_YML = RAIZ.resolve("src/main/resources/application.yml");
	private static final Path MIGRATIONS = RAIZ.resolve("src/main/resources/db/migration");
	private static final Path TASK_DEFINITION = RAIZ.resolve("task-definition.json");

	private static String ler(Path arquivo) throws IOException {
		return Files.readString(arquivo, StandardCharsets.UTF_8);
	}

	// ---------- application.yml principal ----------

	@Test
	void osSegredosSoVemDeVariaveisDeAmbienteESemValorPadrao() throws IOException {
		String yml = ler(APPLICATION_YML);

		assertThat(yml).contains("secret: ${JWT_SECRET}").contains("password: ${DB_PASSWORD}");
		assertThat(yml).doesNotContain("${JWT_SECRET:").doesNotContain("${DB_PASSWORD:");
	}

	@Test
	void todaChaveQueParecesSegredoNoYmlEUmaVariavelDeAmbienteSemValorEscrito() throws IOException {
		// só chaves que TERMINAM em password, secret, token ou key (assim "access-token-expiration: 15m", que é uma
		// duração, não é confundida com um segredo)
		Pattern chave = Pattern.compile("^\\s*([A-Za-z0-9.-]*?(?:password|secret|token|key))\\s*:\\s*(\\S.*)$",
				Pattern.CASE_INSENSITIVE);
		// ${VARIAVEL} (sem padrão) ou ${VARIAVEL:} (padrão vazio = desligado). Nada de ${VARIAVEL:algum-valor}.
		Pattern aceito = Pattern.compile("^\\$\\{[A-Z][A-Z0-9_]*:?}$");
		List<String> suspeitas = new ArrayList<>();
		for (String linha : ler(APPLICATION_YML).split("\\R")) {
			if (linha.strip().startsWith("#")) {
				continue;
			}
			Matcher m = chave.matcher(linha);
			if (m.matches() && !aceito.matcher(m.group(2).strip()).matches()) {
				suspeitas.add(linha.strip());
			}
		}
		assertThat(suspeitas).as("chaves com cara de segredo e valor escrito no application.yml").isEmpty();
	}

	@Test
	void ocorsEOPrimeiroAdminSaoDesligadosPorPadrao() throws IOException {
		String yml = ler(APPLICATION_YML);

		assertThat(yml).contains("allowed-origins: ${CORS_ALLOWED_ORIGINS:}");
		assertThat(yml).contains("password: ${ADMIN_BOOTSTRAP_PASSWORD:}");
	}

	// ---------- task-definition.json (versionado: vai para a AWS) ----------

	@Test
	void nenhumSegredoNaTaskDefinitionEstaEscritoEmEnvironment() throws IOException {
		JsonNode raiz = JsonMapper.builder().build().readTree(ler(TASK_DEFINITION));
		Pattern nomeDeSegredo = Pattern.compile(".*(PASSWORD|SECRET|TOKEN|KEY).*", Pattern.CASE_INSENSITIVE);

		for (JsonNode container : raiz.get("containerDefinitions")) {
			for (JsonNode variavel : container.get("environment")) {
				String nome = variavel.get("name").asString();
				assertThat(nomeDeSegredo.matcher(nome).matches())
						.as("a variável %s parece um segredo e está em 'environment' (deveria estar em 'secrets')", nome).isFalse();
				assertThat(variavel.get("value").asString().toLowerCase()).as("valor de %s", nome)
						.doesNotContain("password=").doesNotContain("pwd=");
			}
			for (JsonNode segredo : container.get("secrets")) {
				assertThat(segredo.get("valueFrom").asString()).as("segredo %s", segredo.get("name").asString())
						.startsWith("arn:aws:secretsmanager:");
			}
		}
	}

	// ---------- migrations ----------

	@Test
	void soAV5SemeiaUsuarioEAV6RemoveExatamenteEsseLoginConhecido() throws IOException {
		List<String> quemInsereUsuario = new ArrayList<>();
		try (Stream<Path> arquivos = Files.list(MIGRATIONS)) {
			for (Path arquivo : arquivos.sorted().toList()) {
				if (ler(arquivo).toUpperCase().contains("INSERT INTO USERS")) {
					quemInsereUsuario.add(arquivo.getFileName().toString());
				}
			}
		}
		// uma migration nova que semear outro usuário (com senha conhecida) precisa ser uma decisão consciente
		assertThat(quemInsereUsuario).as("migrations que inserem em 'users'")
				.containsExactly("V5__seed_permissions_and_admin.sql");

		Pattern hash = Pattern.compile("'(\\{bcrypt}\\$2[aby]\\$[0-9]{2}\\$[A-Za-z0-9./]{53})'");
		Matcher naV5 = hash.matcher(ler(MIGRATIONS.resolve("V5__seed_permissions_and_admin.sql")));
		Matcher naV6 = hash.matcher(ler(MIGRATIONS.resolve("V6__remove_seeded_admin.sql")));
		assertThat(naV5.find()).as("hash semeado na V5").isTrue();
		assertThat(naV6.find()).as("hash removido na V6").isTrue();
		// se não forem idênticos, a V6 não removeria nada e o login conhecido iria para o ambiente publicado
		assertThat(naV6.group(1)).isEqualTo(naV5.group(1));
		assertThat(ler(MIGRATIONS.resolve("V6__remove_seeded_admin.sql"))).contains("DELETE FROM users")
				.contains("user_name = 'admin'");
	}

	@Test
	void nenhumaMigrationTemSenhaEmTextoPuro() throws IOException {
		try (Stream<Path> arquivos = Files.list(MIGRATIONS)) {
			for (Path arquivo : arquivos.toList()) {
				String sql = ler(arquivo).toLowerCase();
				if (sql.contains("insert into users")) {
					// a senha de cada usuário semeado tem que ser um hash {bcrypt}
					assertThat(sql).as(arquivo.getFileName().toString()).contains("'{bcrypt}$2");
				}
			}
		}
	}

}
