package br.com.vitor.apicrud_alunos.config;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Documentação da API (OpenAPI 3 / Swagger UI). Só fica no ar com SWAGGER_ENABLED=true (ver application.yml).
// As anotações de cada rota ficam nas interfaces de controller.docs; aqui estão o texto geral, o esquema de segurança
// (botão Authorize) e os grupos do seletor.
@Configuration
public class OpenApiConfig {

	public static final String ESQUEMA_BEARER = "bearerAuth";

	private static final String DESCRICAO = """
			API de cadastro de **alunos** (CRUD) com autenticação por token **JWT**.

			### Como autenticar
			1. Chame **POST /auth/signin** (grupo *Autenticação*) com `username` e `password`.
			2. Copie o valor de **accessToken** da resposta (vale 15 minutos).
			3. Clique em **Authorize** (cadeado), cole **só o token** (sem a palavra `Bearer`) e confirme.
			4. Quando o token vencer, use **POST /auth/refresh** com o `refreshToken` (vale 24 horas) ou entre de novo.

			### Quem pode o quê
			- **USER**: consultar, criar, editar, ativar e desativar alunos.
			- **ADMIN**: tudo o que o USER faz, apagar alunos e cadastrar usuários (**POST /auth/register**).
			- Sem token: **401**. Com token, mas sem permissão: **403**.

			### Formatos de resposta
			- A listagem e a busca por id respondem em **HAL** (`application/hal+json`): os alunos vêm em `_embedded.alunos`, \
			os links em `_links` e os dados de paginação em `page`. Os demais endpoints respondem JSON simples.
			- Os erros seguem sempre o mesmo formato: `dataHora`, `status`, `erro` e `mensagem`.
			- Se o banco de dados estiver fora do ar, **qualquer rota** responde **503** (com o cabeçalho `Retry-After`): tente de novo em instantes.
			- Datas são `aaaa-mm-dd` e precisam existir de verdade: `2000-02-30` é recusada com **400** (não é corrigida em silêncio).

			### Paginação e ordenação
			`GET /alunos?page=0&size=20&sort=nome,asc`. O tamanho padrão é 20 e o máximo é 50 (valores maiores são reduzidos). \
			Os campos de ordenação aceitos são os do aluno (`id`, `nome`, `email`, `dataNascimento`, `ativo`); outro campo dá **400**. \
			Filtros: `nome` (contém, sem diferenciar maiúsculas) e `ativo` (`true` ou `false`).""";

	@Bean
	public OpenAPI apiAlunos() {
		return new OpenAPI()
				.info(new Info().title("API de Alunos").version("v1").description(DESCRICAO))
				.components(new Components().addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
						.type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
						.description("Cole o accessToken devolvido por POST /auth/signin (sem a palavra Bearer).")))
				// por padrão toda rota pede o token; /auth/signin e /auth/refresh anulam isso (@SecurityRequirements vazio)
				.addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
	}

	// O 503 (banco fora do ar) pode vir de QUALQUER rota: documentado aqui, uma vez, para todas as operações
	@Bean
	public OperationCustomizer respostaDeServicoIndisponivel() {
		return (operation, metodo) -> {
			if (operation.getResponses() != null && !operation.getResponses().containsKey("503")) {
				operation.getResponses().addApiResponse("503", new ApiResponse()
						.description("Banco de dados fora do ar: tente novamente em instantes (veja o cabeçalho Retry-After)")
						.addHeaderObject("Retry-After", new Header().description("Segundos para esperar antes de tentar de novo")
								.schema(new IntegerSchema()))
						.content(new Content().addMediaType("application/json",
								new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
			}
			return operation;
		};
	}

	// Um grupo por recurso: aparecem no seletor "Select a definition" do Swagger UI
	@Bean
	public GroupedOpenApi grupoAlunos() {
		return GroupedOpenApi.builder().group("alunos").displayName("Alunos").pathsToMatch("/alunos/**")
				.addOperationCustomizer(respostaDeServicoIndisponivel()).build();
	}

	@Bean
	public GroupedOpenApi grupoAutenticacao() {
		return GroupedOpenApi.builder().group("autenticacao").displayName("Autenticação").pathsToMatch("/auth/**")
				.addOperationCustomizer(respostaDeServicoIndisponivel()).build();
	}

}
