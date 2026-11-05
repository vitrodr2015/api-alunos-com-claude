package br.com.vitor.apicrud_alunos.controller.docs;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;

import br.com.vitor.apicrud_alunos.dto.AlunoRequest;
import br.com.vitor.apicrud_alunos.dto.AlunoResponse;
import br.com.vitor.apicrud_alunos.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

// Só a DOCUMENTAÇÃO do AlunoController (Swagger). O controller implementa esta interface e fica apenas com a lógica.
// Quem pode o quê está no SecurityConfig: leitura/criar/editar/ativar/desativar = USER ou ADMIN; apagar = só ADMIN.
@Tag(name = "Alunos", description = "CRUD de alunos. Exige token. Apagar é só do ADMIN; as demais rotas são de USER ou ADMIN.")
public interface AlunoControllerDocs {

	@Operation(summary = "Criar aluno", description = "Cria um aluno ativo. O e-mail é único (sem diferenciar maiúsculas). "
			+ "A resposta traz o cabeçalho **Location** com o endereço do aluno criado.")
	@ApiResponse(responseCode = "201", description = "Aluno criado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos (nome, e-mail ou data de nascimento) ou JSON quebrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe aluno com este e-mail",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	ResponseEntity<AlunoResponse> criar(AlunoRequest request);

	@Operation(summary = "Listar alunos", description = "Lista paginada em **HAL** (`_embedded.alunos`, `_links` e `page`). "
			+ "Padrão: página 0, 20 por página (máximo 50). Ordene com `sort=campo,asc|desc`.")
	@ApiResponse(responseCode = "200", description = "Página de alunos")
	@ApiResponse(responseCode = "400", description = "Campo de ordenação inválido ou parâmetro mal formado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	PagedModel<EntityModel<AlunoResponse>> listar(
			@Parameter(description = "Parte do nome (contém, sem diferenciar maiúsculas). Ex.: ana") String nome,
			@Parameter(description = "Filtra por situação: true (ativos) ou false (inativos). Sem o parâmetro, traz todos.") Boolean ativo,
			@ParameterObject Pageable pageable);

	@Operation(summary = "Buscar aluno por id", description = "Responde em **HAL**, com `_links`.")
	@ApiResponse(responseCode = "200", description = "Aluno encontrado")
	@ApiResponse(responseCode = "400", description = "Id inválido (zero, negativo ou não numérico)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Aluno não encontrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	EntityModel<AlunoResponse> buscar(@Parameter(description = "Id do aluno", example = "1") Long id);

	@Operation(summary = "Atualizar aluno (substitui todos os campos)")
	@ApiResponse(responseCode = "200", description = "Aluno atualizado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos ou id inválido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Aluno não encontrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "O e-mail já pertence a outro aluno",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	AlunoResponse atualizar(@Parameter(description = "Id do aluno", example = "1") Long id, AlunoRequest request);

	@Operation(summary = "Desativar aluno", description = "Marca o aluno como inativo (não apaga). Repetir a chamada não muda nada.")
	@ApiResponse(responseCode = "200", description = "Aluno desativado")
	@ApiResponse(responseCode = "400", description = "Id inválido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Aluno não encontrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	AlunoResponse desativar(@Parameter(description = "Id do aluno", example = "1") Long id);

	@Operation(summary = "Reativar aluno", description = "Marca o aluno como ativo. Repetir a chamada não muda nada.")
	@ApiResponse(responseCode = "200", description = "Aluno reativado")
	@ApiResponse(responseCode = "400", description = "Id inválido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Aluno não encontrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	AlunoResponse ativar(@Parameter(description = "Id do aluno", example = "1") Long id);

	@Operation(summary = "Apagar aluno (só ADMIN)", description = "Apaga de vez e não dá para desfazer. Para tirar o aluno de circulação sem apagar, use a desativação.")
	@ApiResponse(responseCode = "204", description = "Aluno apagado")
	@ApiResponse(responseCode = "400", description = "Id inválido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "O usuário não é ADMIN",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "404", description = "Aluno não encontrado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	ResponseEntity<Void> remover(@Parameter(description = "Id do aluno", example = "1") Long id);

}
