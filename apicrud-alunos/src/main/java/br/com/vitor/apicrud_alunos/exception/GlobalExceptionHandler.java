package br.com.vitor.apicrud_alunos.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.vitor.apicrud_alunos.security.InvalidTokenException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	// ordenação por um campo que não existe na entidade (ex.: sort=senha)
	@ExceptionHandler(PropertyReferenceException.class)
	public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex) {
		return build(HttpStatus.BAD_REQUEST,
				"Não é possível ordenar por '" + ex.getPropertyName() + "': esse campo não existe");
	}

	// token vencido, adulterado, de outro emissor, do tipo errado, ou de usuário que não pode mais entrar
	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidToken(InvalidTokenException ex) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
		return build(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Erro inesperado", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
	}

	// Validação do corpo (@Valid): lista os campos inválidos na mensagem
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
				.sorted()
				.collect(Collectors.joining("; "));
		return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, message), headers, status, request);
	}

	// Erros do próprio Spring MVC (400, 404, 405, 406, 415...) saem no mesmo formato
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		String message = switch (ex) {
			case TypeMismatchException e -> "O valor '" + e.getValue() + "' é inválido para '" + e.getPropertyName() + "'";
			case HttpMessageNotReadableException _ -> "Corpo da requisição ausente ou ilegível";
			case MissingServletRequestParameterException e -> "O parâmetro '" + e.getParameterName() + "' é obrigatório";
			case HttpRequestMethodNotSupportedException _ -> "Método HTTP não permitido nesta rota";
			case HttpMediaTypeNotSupportedException _ ->
				"Formato do corpo não suportado. Envie application/json, application/xml ou application/yaml";
			case HttpMediaTypeNotAcceptableException _ ->
				"Formato de resposta não suportado. Esta API responde application/json, application/xml ou application/yaml";
			case NoResourceFoundException _ -> "Rota não encontrada";
			default -> body instanceof ProblemDetail problem && problem.getDetail() != null
					? problem.getDetail()
					: descricao(status);
		};
		return ResponseEntity.status(status).headers(headers).contentType(MediaType.APPLICATION_JSON)
				.body(toResponse(status, message));
	}

	private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(toResponse(status, message));
	}

	private ErrorResponse toResponse(HttpStatus status, String message) {
		return new ErrorResponse(Instant.now(), status.value(), descricao(status), message);
	}

	private String descricao(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> "Requisição inválida";
			case UNAUTHORIZED -> "Não autenticado";
			case NOT_FOUND -> "Não encontrado";
			case METHOD_NOT_ALLOWED -> "Método não permitido";
			case NOT_ACCEPTABLE -> "Formato não aceito";
			case CONFLICT -> "Conflito";
			case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
			case INTERNAL_SERVER_ERROR -> "Erro interno";
			default -> status.getReasonPhrase();
		};
	}

}
