package br.com.vitor.apicrud_alunos.controller.docs;

import org.springframework.http.ResponseEntity;

import br.com.vitor.apicrud_alunos.dto.RefreshRequest;
import br.com.vitor.apicrud_alunos.dto.RegisterRequest;
import br.com.vitor.apicrud_alunos.dto.SignInRequest;
import br.com.vitor.apicrud_alunos.dto.TokenResponse;
import br.com.vitor.apicrud_alunos.dto.UserResponse;
import br.com.vitor.apicrud_alunos.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

// Só a DOCUMENTAÇÃO do AuthController (Swagger). signin e refresh são públicos (sem cadeado); register é só do ADMIN.
@Tag(name = "Autenticação", description = "Login, renovação do token e cadastro de usuários. Só JSON.")
public interface AuthControllerDocs {

	@Operation(summary = "Entrar (login)", description = "Devolve o **accessToken** (15 minutos) e o **refreshToken** (24 horas). "
			+ "Copie o accessToken e use o botão **Authorize**. Qualquer falha de login responde igual (401), "
			+ "sem revelar se o usuário existe.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Login feito")
	@ApiResponse(responseCode = "400", description = "Usuário ou senha em branco, ou grandes demais",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Usuário ou senha inválidos",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	ResponseEntity<TokenResponse> signIn(SignInRequest pedido);

	@Operation(summary = "Renovar o token", description = "Troca um **refreshToken** válido por um par novo. "
			+ "O accessToken não serve aqui, e o refreshToken não serve nas outras rotas.")
	@SecurityRequirements
	@ApiResponse(responseCode = "200", description = "Par de tokens novo")
	@ApiResponse(responseCode = "400", description = "refreshToken em branco",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "refreshToken inválido, vencido, do tipo errado ou de usuário desativado",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	ResponseEntity<TokenResponse> refresh(RefreshRequest pedido);

	@Operation(summary = "Cadastrar usuário (só ADMIN)", description = "Cria um usuário com a permissão **USER**. "
			+ "A senha nunca é devolvida. Dar a permissão ADMIN a alguém não é possível por aqui.")
	@ApiResponse(responseCode = "201", description = "Usuário criado")
	@ApiResponse(responseCode = "400", description = "Dados inválidos (nome de usuário, nome completo ou senha de 8 a 72 caracteres)",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "401", description = "Sem token, token inválido ou vencido",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "403", description = "O usuário não é ADMIN",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	@ApiResponse(responseCode = "409", description = "Já existe um usuário com este nome",
			content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
	ResponseEntity<UserResponse> register(RegisterRequest pedido);

}
